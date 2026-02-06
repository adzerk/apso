package com.kevel.apso.io

import java.io.InputStream
import java.net.URI

import com.google.cloud.storage.{Blob, Storage, StorageOptions}

import com.kevel.apso.gcp.GCSBucket

case class GCSFileDescriptor(
    bucket: GCSBucket,
    protected val elements: List[String],
    private var summary: Option[Blob] = None
) extends FileDescriptor
    with RemoteFileDescriptor {

  type Self = GCSFileDescriptor

  val bucketName: String = bucket.bucketName
  protected val root: String = bucketName

  private lazy val builtPath: String = buildPath(elements)
  @inline private def buildPath(p: Seq[String]): String = p.mkString("/")

  protected def duplicate(elements: List[String]): GCSFileDescriptor = this.copy(elements = elements)

  def uri: URI = new URI(s"gs://$path")

  def size: Long = summary match {
    case Some(info) => Option(info.getSize).map(_.toLong).getOrElse(0L)
    case None       => bucket.size(builtPath)
  }

  def lastModifiedTimestamp: Long =
    summary.map(_.getUpdateTimeOffsetDateTime.toInstant.toEpochMilli).getOrElse(bucket.lastModified(builtPath))

  def download(localTarget: LocalFileDescriptor, safeDownloading: Boolean): Boolean = {
    if (localTarget.isDirectory) throw new Exception("File descriptor points to a directory")
    else {
      localTarget.parent().mkdirs()
      if (safeDownloading) {
        val tmpFile = localTarget.sibling(_ + ".tmp")
        val succeed = bucket.pull(builtPath, tmpFile.path)
        if (succeed) tmpFile.rename(localTarget)
        succeed
      } else bucket.pull(builtPath, localTarget.path)
    }
  }

  def upload(localTarget: LocalFileDescriptor): Boolean = {
    if (localTarget.isDirectory) throw new Exception("File descriptor points to a directory")
    else bucket.push(builtPath, localTarget.file)
  }

  def upload(inputStream: InputStream, length: Option[Long]): Boolean = {
    if (isDirectory) throw new Exception("File descriptor points to a directory")
    else bucket.push(builtPath, inputStream, length)
  }

  def stream(offset: Long = 0L): InputStream = bucket.stream(builtPath, offset)

  override def cd(pathString: String): GCSFileDescriptor = {
    val newPath = pathString.split("/").map(_.trim).toList.foldLeft(elements) {
      case (acc, "." | "") => acc
      case (acc, "..")     => acc.dropRight(1)
      case (acc, segment)  => acc :+ segment
    }
    this.copy(elements = newPath)
  }

  override def list: Iterator[GCSFileDescriptor] = {
    val prefix = elements.mkString("/")
    bucket
      .getFilesInFolder(prefix)
      .map { case (key, info) =>
        this.copy(elements = key.split("/").toList, summary = info)
      }
  }

  def listAllFilesWithPrefix(prefix: String): Iterator[GCSFileDescriptor] = {
    bucket.getObjectsWithMatchingPrefix(buildPath(elements :+ prefix), includeDirectories = false).map { blob =>
      this.copy(elements = blob.getName.split("/").toList, summary = Some(blob))
    }
  }

  override def sibling(f: String => String): GCSFileDescriptor =
    GCSFileDescriptor(bucket, elements.dropRight(1) :+ f(name))

  private lazy val isDirectoryRemote: Boolean =
    elements.isEmpty || bucket.isDirectory(builtPath)

  private lazy val isBucketAndExists: Boolean =
    elements.isEmpty && bucket.bucketExists

  private var isDirectoryLocal: Boolean = false

  def isDirectory: Boolean = isDirectoryLocal || isDirectoryRemote

  def exists: Boolean = bucket.exists(builtPath) || isDirectory || isBucketAndExists

  def delete(): Boolean = bucket.delete(builtPath)

  def mkdirs(): Boolean = {
    val result = isDirectory || bucket.createDirectory(builtPath)
    isDirectoryLocal = result
    result
  }

  override def toString: String = uri.toString
}

object GCSFileDescriptor {

  /** Creates a GCSFileDescriptor from "bucket/path/..." using the default storage instance with Application Default
    * Credentials (ADC).
    */
  def apply(path: String): GCSFileDescriptor = {
    path.split('/').toList match {
      case gcsBucket :: gcsPath =>
        // Application Default Credentials (ADC)
        val bucket = new GCSBucket(gcsBucket, StorageOptions.getDefaultInstance.getService)
        GCSFileDescriptor(bucket, gcsPath.filterNot(_.trim.isEmpty))
      case _ =>
        throw new IllegalArgumentException("Error parsing GCS URI")
    }
  }

  /** Creates a GCSFileDescriptor from "bucket/path/..." and a GCS Storage instance.
    */
  def apply(path: String, mkStorage: () => Storage): GCSFileDescriptor = {
    path.split('/').toList match {
      case gcsBucket :: gcsPath =>
        val bucket = new GCSBucket(gcsBucket, mkStorage)
        GCSFileDescriptor(bucket, gcsPath.filterNot(_.trim.isEmpty))
      case _ =>
        throw new IllegalArgumentException("Error parsing GCS URI")
    }
  }
}
