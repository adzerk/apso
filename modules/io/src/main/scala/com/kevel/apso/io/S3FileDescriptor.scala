package com.kevel.apso.io

import java.io.InputStream
import java.net.URI

import scala.collection.concurrent.TrieMap

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.services.s3.model.S3Object

import com.kevel.apso.aws.{S3Bucket, SerializableAWSCredentials}

case class S3FileDescriptor(
    bucket: S3Bucket,
    protected val elements: List[String],
    private var summary: Option[S3Object] = None
) extends FileDescriptor
    with RemoteFileDescriptor {

  type Self = S3FileDescriptor

  val bucketName = bucket.bucketName

  protected val root = bucketName

  private lazy val builtPath = buildPath(elements)

  @inline private def buildPath(p: Seq[String]): String = p.mkString("/")

  protected def duplicate(elements: List[String]) =
    this.copy(elements = elements)

  def uri: URI =
    new URI(s"s3://$path")

  def size = summary match {
    case Some(info) => info.size
    case None       => bucket.size(builtPath)
  }

  def lastModifiedTimestamp = summary.fold(bucket.lastModified(builtPath))(_.lastModified.toEpochMilli)

  def download(localTarget: LocalFileDescriptor, safeDownloading: Boolean): Boolean = {
    if (localTarget.isDirectory) {
      throw new Exception("File descriptor points to a directory")
    } else {

      localTarget.parent().mkdirs()

      if (safeDownloading) {
        val tmpFile = localTarget.sibling(_ + ".tmp")
        val succeed = bucket.pull(builtPath, tmpFile.path)
        if (succeed) tmpFile.rename(localTarget)
        succeed
      } else {
        bucket.pull(builtPath, localTarget.path)
      }
    }
  }

  def upload(localTarget: LocalFileDescriptor): Boolean = {
    if (localTarget.isDirectory) {
      throw new Exception("File descriptor points to a directory")
    } else {
      bucket.push(builtPath, localTarget.file)
    }
  }

  def upload(inputStream: InputStream, length: Option[Long]): Boolean = {
    if (isDirectory) {
      throw new Exception("File descriptor points to a directory")
    } else {
      bucket.push(builtPath, inputStream, length)
    }
  }

  def stream(offset: Long = 0L) = bucket.stream(builtPath, offset)

  override def cd(pathString: String): S3FileDescriptor = {
    val newPath = pathString.split("/").map(_.trim).toList.foldLeft(elements) {
      case (acc, "." | "") => acc
      case (acc, "..")     => acc.dropRight(1)
      case (acc, segment)  => acc :+ segment
    }
    this.copy(elements = newPath)
  }

  override def list: Iterator[S3FileDescriptor] = {
    val prefix = elements.mkString("/")
    bucket
      .getFilesInFolder(prefix)
      .map({ case (key, info) =>
        this.copy(elements = elements :+ key.stripPrefix(prefix).stripPrefix("/").stripSuffix("/"), summary = info)
      })
  }

  def listAllFilesWithPrefix(prefix: String): Iterator[S3FileDescriptor] = {
    listS3WithPrefix(prefix, includeDirectories = false).map { info =>
      this.copy(elements = info.key.split("/").toList, summary = Some(info))
    }
  }

  private[this] def listS3WithPrefix(prefix: String, includeDirectories: Boolean): Iterator[S3Object] = {
    bucket.getObjectsWithMatchingPrefix(buildPath(elements :+ prefix), includeDirectories)
  }

  override def sibling(f: String => String): S3FileDescriptor = {
    S3FileDescriptor(bucket, elements.dropRight(1) :+ f(name))
  }

  private lazy val isDirectoryRemote = {
    // the bucket itself is considered a directory
    if (elements.isEmpty) true
    else bucket.isDirectory(builtPath)
  }
  private lazy val isBucketAndExists = elements.isEmpty && bucket.bucketExists
  private var isDirectoryLocal = false
  def isDirectory: Boolean = isDirectoryLocal || isDirectoryRemote

  def exists: Boolean = bucket.exists(builtPath) || isDirectory || isBucketAndExists

  def delete(): Boolean = bucket.delete(builtPath)

  def mkdirs(): Boolean = {
    val result = isDirectory || bucket.createDirectory(builtPath)
    isDirectoryLocal = result
    result
  }

  override def toString: String =
    uri.toString
}

object S3FileDescriptor {

  /** Creates an S3FileDescriptor from a path string extracting the bucket and the path
    * @param path
    *   the uri without the protocol, containing the bucket and path
    * @return
    *   a s3 file descriptor
    */
  def apply(path: String): S3FileDescriptor = apply(path, None)

  /** Creates an S3FileDescriptor from a path string extracting the bucket and the path
    * @param path
    *   the uri without the protocol, containing the bucket and path
    * @param credentialsConfig
    *   the config containing the credentials
    * @return
    *   a s3 file descriptor
    */
  def apply(path: String, credentialsConfig: config.Credentials.S3): S3FileDescriptor =
    apply(path, credentials.read(credentialsConfig, path))

  /** Creates an S3FileDescriptor from a path string extracting the bucket and the path
    * @param path
    *   the uri without the protocol, containing the bucket and path
    * @param credentials
    *   credentials for accessing the s3 bucket
    * @return
    *   a s3 file descriptor
    */
  def apply(path: String, credentials: AwsBasicCredentials): S3FileDescriptor = {
    apply(path, Some(SerializableAWSCredentials(credentials)))
  }

  /** Creates an S3FileDescriptor from a path string extracting the bucket and the path
    * @param path
    *   the uri without the protocol, containing the bucket and path
    * @param credentials
    *   serializable credentials for accessing the s3 bucket
    * @return
    *   a s3 file descriptor
    */
  def apply(path: String, credentials: SerializableAWSCredentials): S3FileDescriptor = {
    apply(path, Some(credentials))
  }

  /** Creates an S3FileDescriptor from a path string extracting the bucket and the path
    * @param path
    *   the uri without the protocol, containing the bucket and path
    * @param credentials
    *   optional credentials for accessing the s3 bucket
    * @return
    *   a s3 file descriptor
    */
  private def apply(path: String, credentials: Option[SerializableAWSCredentials]): S3FileDescriptor = {
    path.split('/').toList match {
      case s3bucket :: s3path =>
        def newBucket = credentials.fold(new S3Bucket(s3bucket)) { s3Cred =>
          new S3Bucket(s3bucket, () => StaticCredentialsProvider.create(s3Cred))
        }
        val s3BucketRef = s3Buckets.getOrElseUpdate(s3bucket, newBucket)
        S3FileDescriptor(s3BucketRef, s3path.filterNot(_.trim == ""))

      case _ => throw new IllegalArgumentException("Error parsing S3 URI")
    }
  }

  /** Credential extractor for a s3 bucket from the credential config
    */
  val credentials = new FileDescriptorCredentials[config.Credentials.S3.Entry, SerializableAWSCredentials] {
    def id(path: String) = path.split("/").headOption.mkString

    def createCredentials(id: String, s3Config: config.Credentials.S3.Entry) = {
      new SerializableAWSCredentials(s3Config.accessKey, s3Config.secretKey)
    }
  }

  /** This Map caches S3Buckets so that each S3FileDescriptor does not need to have it's own internal object to access
    * the bucket.
    */
  private val s3Buckets = TrieMap.empty[String, S3Bucket]
}
