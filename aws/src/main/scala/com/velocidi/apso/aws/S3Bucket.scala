package com.velocidi.apso.aws

import java.io._
import java.nio.file.Paths

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

import com.typesafe.config.ConfigFactory
import software.amazon.awssdk.auth.credentials._
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{
  CopyObjectRequest,
  CreateBucketRequest,
  DeleteObjectRequest,
  GetObjectRequest,
  HeadBucketRequest,
  HeadObjectRequest,
  ListObjectsRequest,
  ObjectCannedACL,
  PutObjectAclRequest,
  PutObjectRequest,
  S3Exception,
  S3Object
}
import software.amazon.awssdk.utils.IoUtils

import com.velocidi.apso.Logging

/** A representation of an Amazon's S3 bucket. This class wraps an `AmazonS3Client` and provides a higher level
  * interface for pushing and pulling files to and from a bucket.
  *
  * @param bucketName
  *   the name of the bucket
  * @param credentialsProvider
  *   optional AWS credentials provider to use (since AWSCredentials are not serializable). If the parameter is not
  *   supplied, they will be retrieved from the [[CredentialStore]].
  */
class S3Bucket(
    val bucketName: String,
    private val credentialsProvider: () => AwsCredentialsProvider = CredentialStore.providerChain
) extends Logging
    with Serializable {

  private[this] lazy val config = ConfigFactory.load()

  private[this] lazy val configPrefix = "aws.s3"
  private[this] lazy val region = Try(config.getString(configPrefix + ".region"))

  @transient private[this] var _s3: S3Client = _

  private[this] def s3 = {
    if (_s3 == null) {
      _s3 = S3Client
        .builder()
        .region(region.map(Region.of).getOrElse(Region.US_EAST_1))
        .credentialsProvider(credentialsProvider())
        .build()

      // Create bucket if it does not yet exist
      val res = _s3.headBucket(HeadBucketRequest.builder().bucket(bucketName).build()).sdkHttpResponse()
      if (!res.isSuccessful) {
        _s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
      }
    }
    _s3
  }

  private[this] def splitKey(key: String) = {
    val iExtensionPoint = key.lastIndexOf('.')
    val iSlash = key.lastIndexOf('/')
    val mainKey = if (iSlash != -1) key.substring(0, iSlash) else ""
    val extension = if (iExtensionPoint != -1) key.substring(iExtensionPoint) else ""
    val name = if (iExtensionPoint != -1) key.substring(0, iExtensionPoint) else key

    (mainKey, name, extension)
  }

  private[this] def sanitizeKey(key: String) = if (key.startsWith("./")) key.drop(2) else key

  /** Returns size of the file in the location specified by `key` in the bucket. If the file doesn't exist the return
    * value is 0.
    *
    * @param key
    *   the remote pathname for the file
    * @return
    *   the size of the file in the location specified by `key` in the bucket if the exists, 0 otherwise.
    */
  def size(key: String): Long = retry {
    s3.getObject(
      GetObjectRequest
        .builder()
        .bucket(bucketName)
        .key(key)
        .build()
    ).response()
      .metadata()
      .get(S3Bucket.contentLengthMetadataKey)
      .toLong
  }.getOrElse(0)

  /** Returns the last modified timestamp of the file in the location specified by `key` in the bucket. If the file
    * doesn't exist the return value is 0L. The decision to return 0L in those cases is to align with
    * `java.io.File#lastModified()`'s behavior.
    *
    * @param key
    *   the remote pathname for the file
    * @return
    *   the last modified timestamp of the file in the location specified by `key` in the bucket if it exists, 0L
    *   otherwise.
    */
  def lastModified(key: String): Long =
    retry(s3.getObjectMetadata(bucketName, key).getLastModified().getTime()).getOrElse(0)

  /** Returns a list of objects in a bucket matching a given prefix.
    *
    * @param prefix
    *   the prefix to match
    * @return
    *   a list of objects in a bucket matching a given prefix.
    */
  def getObjectsWithMatchingPrefix(prefix: String, includeDirectories: Boolean = false): Iterator[S3Object] = {
    log.info(s"Finding files matching prefix '$prefix'...")

    val res = s3.listObjects(ListObjectsRequest.builder().bucket(bucketName).prefix(sanitizeKey(prefix)).build())

    res.nextMarker()

    val listings = Iterator.iterate(
      s3.listObjects(ListObjectsRequest.builder().bucket(bucketName).prefix(sanitizeKey(prefix)).build())
    ) { listing =>
      if (listing.isTruncated) {
        log.debug("Result is truncated, asking for another batch of objects...")
        s3.listObjects(
          ListObjectsRequest
            .builder()
            .bucket(bucketName)
            .prefix(sanitizeKey(prefix))
            .marker(listing.nextMarker())
            .build()
        )
      } else null
    }

    val objects = listings.takeWhile(_ != null).flatMap(_.contents().asScala)
    if (includeDirectories) objects else objects.filterNot(_.key().endsWith("/"))
  }

  /** Returns a list of filenames and directories in a bucket matching a given prefix.
    *
    * @param prefix
    *   the prefix to match
    * @return
    *   a list of filenames in a bucket matching a given prefix.
    */
  def getFilesWithMatchingPrefix(prefix: String, includeDirectories: Boolean = false): Iterator[String] =
    getObjectsWithMatchingPrefix(prefix, includeDirectories).map(_.key())

  /** Pushes a given local `File` to the location specified by `key` in the bucket.
    *
    * @param key
    *   the remote pathname for the file
    * @param file
    *   the local `File` to push
    * @return
    *   true if the push was successful, false otherwise.
    */
  def push(key: String, file: File): Boolean = retry {
    log.info(s"Pushing file '${file.getPath}' to 's3://$bucketName/$key'")
    s3.putObject(
      PutObjectRequest
        .builder()
        .bucket(bucketName)
        .key(sanitizeKey(key))
        .build(),
      RequestBody.fromFile(file)
    )
  }.isDefined

  /** Pushes a given `InputStream` to the location specified by `key` in the bucket.
    *
    * @param key
    *   the remote pathname for the file
    * @param inputStream
    *   the `InputStream` to push
    * @param length
    *   the content lenght
    * @return
    *   true if the push was successful, false otherwise.
    */
  def push(key: String, inputStream: InputStream, length: Option[Long]): Boolean = retry {
    log.info(s"Pushing to 's3://$bucketName/$key'")

    // The aws sdk v2 `ReguestBody.fromInputStream` requires the stream length to be specified.
    // To get the length, one must unfortunately read the stream
    val bytes = IoUtils.toByteArray(inputStream)
    val streamLength: Long = length.getOrElse(bytes.length)
    val byteArrayInputStream = new ByteArrayInputStream(bytes)

    val metadata = Map(
      (
        S3Bucket.contentLengthMetadataKey,
        streamLength.toString
      )
    )

    s3.putObject(
      PutObjectRequest
        .builder()
        .bucket(bucketName)
        .key(sanitizeKey(key))
        .metadata(metadata.asJava)
        .build(),
      RequestBody.fromInputStream(byteArrayInputStream, streamLength)
    )
  }.isDefined

  /** Deletes the file in the location specified by `key` in the bucket.
    *
    * @param key
    *   the remote pathname for the file
    * @return
    *   true if the deletion was successful, false otherwise.
    */
  def delete(key: String): Boolean = retry {
    s3.deleteObject(
      DeleteObjectRequest
        .builder()
        .bucket(bucketName)
        .key(sanitizeKey(key))
        .build()
    )
  }.isDefined

  /** Checks if the file in the location specified by `key` in the bucket exists. It returns false if just checking for
    * the bucket existence.
    *
    * @param key
    *   the remote pathname for the file
    * @return
    *   true if the file exists, false otherwise.
    */
  def exists(key: String): Boolean = retry {
    key.nonEmpty &&
    s3.headObject(
      HeadObjectRequest
        .builder()
        .bucket(bucketName)
        .key(key)
        .build()
    ).sdkHttpResponse()
      .isSuccessful
  }.getOrElse(false)

  /** Checks if the location specified by `key` is a directory.
    *
    * @param key
    *   the remote pathname to the directory
    * @return
    *   true if the path is a directory, false otherwise.
    */
  def isDirectory(key: String): Boolean = retry {
    s3.listObjects(
      ListObjectsRequest
        .builder()
        .bucket(bucketName)
        .maxKeys(2)
        .prefix(key)
        .build()
    ).contents
      .asScala
  }.exists { _.exists(_.key().startsWith(key + "/")) }

  /** Checks whether the bucket exists
    *
    * @return
    *   true if the bucket exists, false otherwise.
    */
  def bucketExists: Boolean = retry {
    s3.headBucket(
      HeadBucketRequest
        .builder()
        .bucket(bucketName)
        .build()
    ).sdkHttpResponse()
      .isSuccessful
  }.getOrElse(false)

  /** Sets an access control list on a given Amazon S3 object.
    *
    * @param key
    *   the remote pathname for the file
    * @param acl
    *   the `ObjectCannedACL` to be applied to the Amazon S3 object
    */
  def setAcl(key: String, acl: ObjectCannedACL) = {
    log.info(s"Setting 's3://$bucketName/$key' permissions to '$acl'")
    s3.putObjectAcl(
      PutObjectAclRequest
        .builder()
        .bucket(bucketName)
        .key(key)
        .acl(acl)
        .build()
    )
  }

  /** Creates an empty directory at the given `key` location
    *
    * @param key
    *   the remote pathname to the directory
    * @return
    *   true if the directory was created successfully, false otherwise.
    */
  def createDirectory(key: String): Boolean = retry {
    log.info(s"Creating directory in 's3://$bucketName/$key'")
    val metadata = Map((S3Bucket.contentLengthMetadataKey, "0"))

    s3.putObject(
      PutObjectRequest
        .builder()
        .bucket(bucketName)
        .key(sanitizeKey(key) + "/")
        .metadata(metadata.asJava)
        .build(),
      RequestBody.empty()
    )
  }.isDefined

  /** Backups a remote file with the given `key`. A backup consists in copying the supplied file to a backup folder
    * under the same bucket and folder the file is currently in.
    *
    * @param key
    *   the remote pathname to backup
    * @return
    *   true if the backup was successful, false otherwise.
    */
  def backup(key: String): Boolean = retry {
    val sanitizedKey = sanitizeKey(key)
    val (mainKey, name, extension) = splitKey(sanitizedKey)
    val destinationKey = mainKey + "/backup/" + name.substring(mainKey.length + 1) + extension

    s3.copyObject(
      CopyObjectRequest
        .builder()
        .sourceBucket(bucketName)
        .sourceKey(sanitizedKey)
        .destinationBucket(bucketName)
        .destinationKey(destinationKey)
        .build()
    )
  }.isDefined

  /** Pulls a remote file with the given `key`, to the local storage in the pathname provided by `destination`.
    *
    * @param key
    *   the remote pathname to pull from
    * @param destination
    *   the local pathname to pull to
    * @return
    *   true if the pull was successful, false otherwise
    */
  def pull(key: String, destination: String): Boolean = retry {
    log.info(s"Pulling 's3://$bucketName/$key' to '$destination'")
    s3.getObject(
      GetObjectRequest
        .builder()
        .bucket(bucketName)
        .key(sanitizeKey(key))
        .build(),
      Paths.get(destination)
    )
    log.info(s"Downloaded 's3://$bucketName/$key' to '$destination'. Closing files.")
  }.isDefined

  def stream(key: String, offset: Long = 0L): InputStream = {
    log.info(s"Streaming 's3://$bucketName/$key' starting at $offset")
    s3.getObject(
      GetObjectRequest
        .builder()
        .bucket(bucketName)
        .key(sanitizeKey(key))
        .range(offset.toString)
        .build()
    )
  }

  private[this] def handler: PartialFunction[Throwable, Boolean] = {
    case ex: S3Exception =>
      ex.statusCode() match {
        case 404 =>
          log.error("The specified file does not exist", ex); true // no need to retry
        case 403 =>
          log.error("No permission to access the file", ex); true // no need to retry
        case _ =>
          log.error(
            s"""|S3 service error: ${ex.getMessage}. Extended request id: ${ex.extendedRequestId()}
                      |Additional details: ${ex.awsErrorDetails()}""".stripMargin,
            ex
          )
          false
      }

    case ex: AwsServiceException =>
      log.error(s"Service error: ${ex.getMessage}", ex); ex.retryable()

    case ex: SdkClientException
        if ex.getMessage ==
          "Unable to load AWS credentials from any provider in the chain" =>
      log.error("Unable to load AWS credentials", ex); true

    case ex: SdkClientException =>
      log.error("Client error pulling file", ex); ex.retryable()

    case ex: Exception =>
      log.error("An error occurred", ex); false
  }

  private[this] def retry[T](f: => T, tries: Int = 3, sleepTime: Int = 5000): Option[T] =
    if (tries == 0) { log.error("Max retries reached. Aborting S3 operation"); None }
    else
      Try(f) match {
        case Success(res) => Some(res)
        case Failure(e) if !handler(e) =>
          if (tries > 1) {
            log.warn(s"Error during S3 operation. Retrying in ${sleepTime}ms (${tries - 1} more times)")
            Thread.sleep(sleepTime)
          }
          retry(f, tries - 1, sleepTime)

        case _ => None
      }

  override def equals(obj: Any): Boolean = obj match {
    case b: S3Bucket => b.bucketName == bucketName
    case _           => false
  }
}

object S3Bucket {
  private final val contentLengthMetadataKey = "Content-Length"
}
