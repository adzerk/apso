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

  def cd(pathString: String): S3FileDescriptor = {
    val newPath = pathString.split("/").toList.foldLeft(paths) {
      case (acc, "." | "") => acc
      case (acc, "..") => acc.dropRight(1)
      case (acc, segment) => acc :+ segment
    }
    this.copy(paths = newPath)
  }

  override def addChildren(children: List[String]): S3FileDescriptor =
    this.copy(paths = paths ++ children)

  override def toString: String = s"s3://$path"
}

object S3FileDescriptor {

  def apply(path: String): S3FileDescriptor = {
    path.split('/').toList match {
      case s3bucket :: s3path =>
        val s3BucketRef = s3Buckets.getOrElseUpdate(s3bucket, new S3Bucket(s3bucket))
        S3FileDescriptor(s3BucketRef, s3path)

      case _ => throw new Exception("Error parsing S3 URI")
    }
  }

  private val s3Buckets = TrieMap.empty[String, S3Bucket]
}
