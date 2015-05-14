package eu.shiftforward.apso.io

import com.amazonaws.auth.BasicAWSCredentials
import com.typesafe.config.Config
import eu.shiftforward.apso.Logging
import eu.shiftforward.apso.aws.S3Bucket
import eu.shiftforward.apso.config.FileDescriptorCredentials

import scala.collection.concurrent.TrieMap

case class S3FileDescriptor(private val bucket: S3Bucket, private val elements: List[String])
    extends FileDescriptor with Logging {

  lazy val bucketName = bucket.bucketName

  lazy val path: String = bucketName + elements.foldLeft("")((acc, p) => s"$acc/$p")

  lazy val name: String = elements.lastOption.getOrElse("")

  private lazy val builtPath = buildPath(elements)

  @inline private def buildPath(p: Seq[String]): String = p.mkString("/")

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

  def parent(n: Int): S3FileDescriptor = this.copy(elements = elements.dropRight(n))

  private def sanitize(segment: String): Option[String] = {

    val whiteSpaceValidated = segment.trim match {
      case "" => None
      case str => Some(str)
    }

    whiteSpaceValidated.map {
      _.count(_ == '/') match {
        case 0 => segment
        case 1 if segment.endsWith("/") => segment.dropRight(1)
        case _ => throw new IllegalArgumentException("path cannot contain /")
      }
    }
  }

  override def /(name: String): S3FileDescriptor = child(name)

  def child(name: String): S3FileDescriptor =
    this.copy(elements = elements ++ sanitize(name).toList)

  override def child(name: String, name2: String, names: String*): S3FileDescriptor =
    this.copy(elements = elements ++ (Seq(name, name2) ++ names).flatMap(sanitize))

  override def cd(pathString: String): S3FileDescriptor = {
    val newPath = pathString.split("/").map(_.trim).toList.foldLeft(elements) {
      case (acc, "." | "") => acc
      case (acc, "..") => acc.dropRight(1)
      case (acc, segment) => acc :+ segment
    }
    this.copy(elements = newPath)
  }

  override def list: Iterator[S3FileDescriptor] = listByPrefix("")

  def listByPrefix(prefix: String): Iterator[S3FileDescriptor] = {
    val fullPrefix = if (prefix == "") elements else elements :+ prefix
    bucket.getFilesWithMatchingPrefix(buildPath(fullPrefix)).map {
      f => this.copy(elements = f.split("/").toList)
    }
  }

  override def sibling(f: String => String): S3FileDescriptor = {
    S3FileDescriptor(bucket, elements.dropRight(1) :+ f(name))
  }

  private lazy val isDirectoryRemote = bucket.isDirectory(builtPath)
  private var isDirectoryLocal = false
  def isDirectory: Boolean = isDirectoryLocal || isDirectoryRemote

  def exists: Boolean = bucket.exists(builtPath)

  def delete(): Boolean = bucket.delete(builtPath)

  def mkdirs(): Boolean = {
    val result = isDirectory || bucket.createDirectory(builtPath)
    isDirectoryLocal = result
    result
  }

  override def toString: String = s"s3://$path"
}

object S3FileDescriptor {

  /**
   * Creates an S3FileDescriptor from a path string extracting the bucket and the path
   * @param path the uri without the protocol, containing the bucket and path
   * @return a s3 file descriptor
   */
  def apply(path: String): S3FileDescriptor = apply(path, None)

  /**
   * Creates an S3FileDescriptor from a path string extracting the bucket and the path
   * @param path the uri without the protocol, containing the bucket and path
   * @param credentialsConfig the config containing the credentials
   * @return a s3 file descriptor
   */
  def apply(path: String, credentialsConfig: Config): S3FileDescriptor =
    apply(path, credentials.read(credentialsConfig, path))

  /**
   * Creates an S3FileDescriptor from a path string extracting the bucket and the path
   * @param path the uri without the protocol, containing the bucket and path
   * @param credentials optional credentials for accessing the s3 bucket
   * @return a s3 file descriptor
   */
  def apply(path: String, credentials: Option[BasicAWSCredentials]): S3FileDescriptor = {
    path.split('/').toList match {
      case s3bucket :: s3path =>
        def newBucket = credentials.fold(new S3Bucket(s3bucket))(s3Cred => new S3Bucket(s3bucket, () => s3Cred))
        val s3BucketRef = s3Buckets.getOrElseUpdate(s3bucket, newBucket)
        S3FileDescriptor(s3BucketRef, s3path.filterNot(_.trim == ""))

      case _ => throw new Exception("Error parsing S3 URI")
    }
  }

  /**
   * Credential extractor for a s3 bucket from the credential config
   */
  val credentials = new FileDescriptorCredentials[BasicAWSCredentials] {

    val protocol = "s3"

    def id(path: String) = path.split("/").head

    def createCredentials(s3Config: Config) = {
      new BasicAWSCredentials(
        s3Config.getString("access-key"),
        s3Config.getString("secret-key"))
    }
  }

  /**
   * This Map caches S3Buckets so that each S3FileDescriptor does not need to have it's own
   * internal object to access the bucket.
   */
  private val s3Buckets = TrieMap.empty[String, S3Bucket]
}
