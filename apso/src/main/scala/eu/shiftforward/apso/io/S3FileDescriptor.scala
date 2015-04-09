package eu.shiftforward.apso.io

import java.io.File

import eu.shiftforward.apso.Logging
import eu.shiftforward.apso.aws.S3Bucket

import scala.collection.concurrent.TrieMap

case class S3FileDescriptor(private val bucket: S3Bucket, private val paths: List[String])
    extends FileDescriptor with Logging {

  val bucketName = bucket.bucketName

  val path = bucketName + paths.foldLeft("")((acc, p) => s"$acc/$p")

  def download(localTarget: LocalFileDescriptor): Boolean = {
    val localPath = if (localTarget.isDirectory) {
      paths.lastOption match {
        case Some(filename) => localTarget.addChild(filename).path
        case None => throw new Exception("File not specified")
      }
    } else {
      localTarget.path
    }

    bucket.pull(paths.mkString("/"), localPath)
  }

  def upload(localTarget: LocalFileDescriptor): Boolean = {
    bucket.push(paths.mkString("/"), new File(localTarget.path))
  }

  def parent(n: Int): S3FileDescriptor = this.copy(paths = paths.dropRight(n))

  def addChild(child: String): S3FileDescriptor = this.copy(paths = paths :+ child)

  def addChildren(children: List[String]): S3FileDescriptor =
    this.copy(paths = paths ++ children)

  def cd(pathString: String): S3FileDescriptor = {
    val newPath = pathString.split("/").toList.foldLeft(paths) {
      case (acc, "." | "") => acc
      case (acc, "..") => acc.dropRight(1)
      case (acc, segment) => acc :+ segment
    }
    this.copy(paths = newPath)
  }

  def list(): Iterator[S3FileDescriptor] = listByPrefix("")

  def listByPrefix(prefix: String): Iterator[S3FileDescriptor] = {
    bucket.getFilesWithMatchingPrefix(prefix).map(f => this.copy(paths = f.split("/").toList))
  }

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
        S3FileDescriptor(s3BucketRef, s3path)

      case _ => throw new Exception("Error parsing S3 URI")
    }
  }

  /**
   * This Map caches S3Buckets so that each S3FileDescriptor does not need to have it's own
   * internal object to access the bucket.
   */
  private val s3Buckets = TrieMap.empty[String, S3Bucket]
}
