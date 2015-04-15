package eu.shiftforward.apso.io

import java.io.File

import eu.shiftforward.apso.Logging
import eu.shiftforward.apso.aws.S3Bucket

import scala.collection.concurrent.TrieMap

case class S3FileDescriptor(private val bucket: S3Bucket, private val paths: List[String])
    extends FileDescriptor with Logging {

  lazy val bucketName = bucket.bucketName

  lazy val path: String = bucketName + paths.foldLeft("")((acc, p) => s"$acc/$p")

  lazy val name: String = paths.lastOption.getOrElse(bucketName)

  private lazy val builtPath = buildPath(paths)

  @inline private def buildPath(p: Seq[String]): String = p.mkString("/")

  def download(localTarget: LocalFileDescriptor, safeDownloading: Boolean): Option[LocalFileDescriptor] = {
    if (localTarget.isDirectory) {
      paths.lastOption match {
        case Some(filename) => download(localTarget.addChild(filename))
        case None => throw new Exception("File not specified")
      }
    } else {

      localTarget.mkdirs()

      val result = if (safeDownloading) {
        val tmpFile = localTarget.sibling(_ + ".tmp")
        val succeed = bucket.pull(builtPath, tmpFile.path)
        if (succeed) tmpFile.rename(localTarget)
        succeed
      } else {
        bucket.pull(builtPath, localTarget.path)
      }

      if (result) Some(localTarget) else None
    }
  }

  def upload(localTarget: LocalFileDescriptor): Option[LocalFileDescriptor] = {
    if (bucket.push(builtPath, new File(localTarget.path))) {
      Some(localTarget)
    } else None
  }

  def parent(n: Int): S3FileDescriptor = this.copy(paths = paths.dropRight(n))

  private def sanitize(segment: String): String = segment.count(_ == '/') match {
    case 0 => segment
    case 1 if segment.endsWith("/") => segment.dropRight(1)
    case _ => throw new IllegalArgumentException("path cannot contain /")
  }

  override def /(child: String): S3FileDescriptor = addChild(child)

  def addChild(child: String): S3FileDescriptor =
    this.copy(paths = paths :+ sanitize(child))

  override def addChildren(children: String*): S3FileDescriptor =
    this.copy(paths = paths ++ children.map(sanitize))

  override def cd(pathString: String): S3FileDescriptor = {
    val newPath = pathString.split("/").map(_.trim).toList.foldLeft(paths) {
      case (acc, "." | "") => acc
      case (acc, "..") => acc.dropRight(1)
      case (acc, segment) => acc :+ segment
    }
    this.copy(paths = newPath)
  }

  def list(): Iterator[S3FileDescriptor] = listByPrefix("")

  def listByPrefix(prefix: String): Iterator[S3FileDescriptor] = {
    val fullPrefix = if (prefix == "") paths else paths :+ prefix
    bucket.getFilesWithMatchingPrefix(buildPath(fullPrefix)).map {
      f => this.copy(paths = f.split("/").toList)
    }
  }

  override def sibling(f: String => String): S3FileDescriptor = {
    S3FileDescriptor(bucket, paths.dropRight(1) :+ f(name))
  }

  def isDirectory: Boolean = bucket.isDirectory(builtPath)

  def exists: Boolean = bucket.exists(builtPath)

  def delete(): Boolean = bucket.delete(builtPath)

  override def toString: String = s"s3://$path"
}

object S3FileDescriptor {

  /**
   * Creates an S3FileDescriptor from a path string extracting the bucket and the path
   * @param path the uri without the protocol, containing the bucket and path
   * @return a s3 file descriptor
   */
  def apply(path: String): S3FileDescriptor = {
    path.split('/').toList match {
      case s3bucket :: s3path =>
        val s3BucketRef = s3Buckets.getOrElseUpdate(s3bucket, new S3Bucket(s3bucket))
        S3FileDescriptor(s3BucketRef, s3path.filterNot(_.trim == ""))

      case _ => throw new Exception("Error parsing S3 URI")
    }
  }

  /**
   * This Map caches S3Buckets so that each S3FileDescriptor does not need to have it's own
   * internal object to access the bucket.
   */
  private val s3Buckets = TrieMap.empty[String, S3Bucket]
}
