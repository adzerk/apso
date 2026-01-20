package com.kevel.apso.aws

import java.io._
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{CompletableFuture, CompletionException, LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try, Using}

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.async.{AsyncRequestBody, AsyncResponseTransformer}
import software.amazon.awssdk.core.exception.{SdkClientException, SdkException}
import software.amazon.awssdk.regions
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.crt.S3CrtRetryConfiguration
import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.transfer.s3.{S3TransferManager, model}

/** A representation of an Amazon's S3 bucket. This class wraps an `S3AsyncClient` and provides a higher level interface
  * for pushing and pulling files to and from a bucket.
  *
  * @param bucketName
  *   the name of the bucket
  * @param credentialsProvider
  *   optional AWS credentials provider to use (since AwsCredentials are not serializable). If the parameter is not
  *   supplied, they will be retrieved from the [[CredentialStore]].
  */
class S3Bucket(
    val bucketName: String,
    private val credentialsProvider: () => AwsCredentialsProvider = () => CredentialStore.credentialsProvider
) extends LazyLogging
    with Serializable {

  private[this] lazy val config = ConfigFactory.load()

  private[this] lazy val configPrefix = "aws.s3"
  private[this] lazy val region = Try(config.getString(configPrefix + ".region"))
  private[this] lazy val maxConnections = Try(config.getInt(configPrefix + ".max-connections"))
  private[this] lazy val maxErrorRetry = Try(config.getInt(configPrefix + ".max-error-retry"))

  @transient private[this] lazy val defaultExecutor = {
    val maxPoolSize = 100;
    val threadCount = new AtomicInteger(0)
    // NOTE: This is the default thread pool used by the `TransferManager`. I had to replicate it here
    // to make sure that threads are daemonized. This could be problematic, e.g. shutting down the
    // JVM before a transfer has finished, although this won't happen in our use case since we
    // always wait for transfers to finish.
    // (This code is replicated from the AWS SDK with the addition of the `setDaemon` call).
    val executor: ThreadPoolExecutor = new ThreadPoolExecutor(
      0,
      maxPoolSize,
      60,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue(1_000),
      (runnable) => {
        val thread = new Thread(runnable)
        thread.setName(s"apso-transfer-manager-${threadCount.getAndIncrement()}")
        thread.setDaemon(true)
        thread
      }
    )
    executor.allowCoreThreadTimeOut(true);
    executor
  }

  @transient private[this] lazy val s3Client = {
    val client = S3AsyncClient
      .crtBuilder()
      .credentialsProvider(credentialsProvider())
      .crossRegionAccessEnabled(true)

    maxConnections.foreach { connections =>
      client.maxConcurrency(connections)
    }

    maxErrorRetry.foreach { maxRetries =>
      client.retryConfiguration(S3CrtRetryConfiguration.builder().numRetries(maxRetries).build())
    }

    val s3 = client.build()
    if (!bucketExists(s3))
      s3.createBucket(
        CreateBucketRequest
          .builder()
          .bucket(bucketName)
          .createBucketConfiguration(
            CreateBucketConfiguration
              .builder()
              .locationConstraint(region.getOrElse(regions.Region.US_EAST_1.id()))
              .build()
          )
          .build()
      ).join()
    s3
  }

  private[this] def bucketExists(s3Client: S3AsyncClient): Boolean = retry {
    try {
      s3Client.headBucket(_.bucket(bucketName)).join()
      true
    } catch {
      case ce: CompletionException if ce.getCause.isInstanceOf[NoSuchBucketException] =>
        false
    }
  }.getOrElse(false)

  private[this] def splitKey(key: String) = {
    val iExtensionPoint = key.lastIndexOf('.')
    val iSlash = key.lastIndexOf('/')
    val mainKey = if (iSlash != -1) key.substring(0, iSlash) else ""
    val extension = if (iExtensionPoint != -1) key.substring(iExtensionPoint) else ""
    val name = if (iExtensionPoint != -1) key.substring(0, iExtensionPoint) else key

    (mainKey, name, extension)
  }

  private[this] def sanitizeKey(key: String) = if (key.startsWith("./")) key.drop(2) else key

  private[this] def listObjectsV2Iterator(req: ListObjectsV2Request) = Iterator
    .iterate(s3Client.listObjectsV2(req).join()) { listing =>
      if (listing.isTruncated) {
        logger.debug("Asking for another batch of objects...")
        s3Client.listObjectsV2(req.toBuilder.continuationToken(listing.nextContinuationToken).build).join()
      } else null
    }
    .takeWhile(_ != null)

  private[this] def getTransferManager = {
    S3TransferManager.builder().s3Client(s3Client).executor(defaultExecutor).build()
  }

  /** Returns size of the file in the location specified by `key` in the bucket. If the file doesn't exist the return
    * value is 0.
    *
    * @param key
    *   the remote pathname for the file
    * @return
    *   the size of the file in the location specified by `key` in the bucket if the exists, 0 otherwise.
    */
  def size(key: String): Long = retry {
    s3Client.headObject(_.bucket(bucketName).key(sanitizeKey(key))).join().contentLength().longValue()
  }.getOrElse(0L)

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
  def lastModified(key: String): Long = retry {
    s3Client.headObject(_.bucket(bucketName).key(sanitizeKey(key))).join().lastModified().toEpochMilli()
  }.getOrElse(0)

  /** Returns a list of objects in a bucket matching a given prefix.
    *
    * @param prefix
    *   the prefix to match
    * @return
    *   a list of objects in a bucket matching a given prefix.
    */
  def getObjectsWithMatchingPrefix(prefix: String, includeDirectories: Boolean = false): Iterator[S3Object] = retry {
    logger.info(s"Finding files matching prefix '$prefix'...")

    val req = ListObjectsV2Request.builder.bucket(bucketName).prefix(sanitizeKey(prefix)).build
    val objects = listObjectsV2Iterator(req).flatMap(_.contents.asScala)

    if (includeDirectories) objects else objects.filterNot(_.key.endsWith("/"))
  }.getOrElse(Iterator.empty)

  // FIXME: If the root directory/prefix was created by the `mkdirs` method (where we create an object with 0 bytes)
  //        that root directory will be present in the results. Evaluate if we should filter it out since it does not
  //        represent a file/folder inside the requested prefix.
  /** Assuming prefix points to a folder, returns all filenames and optional object summaries immediately below that
    * folder. Only actual files will have an S3ObjectSummary, folders won't.
    *
    * @param prefix
    *   the prefix in which to list files
    * @return
    *   a list of filenames and optional object summaries in a bucket directly "below" the provided prefix.
    */
  def getFilesInFolder(prefix: String): Iterator[(String, Option[S3Object])] = retry {
    logger.info(s"Finding files in folder '$prefix'...")

    val sanitizedPrefix = if (prefix.nonEmpty) s"${sanitizeKey(prefix)}/" else ""

    val req =
      ListObjectsV2Request.builder.bucket(bucketName).prefix(sanitizeKey(sanitizedPrefix)).delimiter("/").build

    listObjectsV2Iterator(req)
      .flatMap(listing =>
        listing.contents.asScala
          .map(summary => (summary.key, Some(summary))) ++ listing.commonPrefixes.asScala.map(p => (p.prefix, None))
      )
  }.getOrElse(Iterator.empty)

  /** Returns a list of filenames and directories in a bucket matching a given prefix.
    *
    * @param prefix
    *   the prefix to match
    * @return
    *   a list of filenames in a bucket matching a given prefix.
    */
  def getFilesWithMatchingPrefix(prefix: String, includeDirectories: Boolean = false): Iterator[String] =
    getObjectsWithMatchingPrefix(prefix, includeDirectories).map(_.key)

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
    logger.info(s"Pushing file '${file.getPath}' to 's3://$bucketName/$key'")

    Using(getTransferManager) {
      _.uploadFile(
        model.UploadFileRequest
          .builder()
          .putObjectRequest(_.bucket(bucketName).key(sanitizeKey(key)))
          .source(file)
          .build()
      ).completionFuture().join()
    }.get
  }.isDefined

  /** Pushes a given `InputStream` to the location specified by `key` in the bucket.
    *
    * @param key
    *   the remote pathname for the file
    * @param inputStream
    *   the `InputStream` to push
    * @param length
    *   the content lenght (setting this to `None` can impact performance
    * @return
    *   true if the push was successful, false otherwise.
    */
  def push(key: String, inputStream: InputStream, length: Option[Long]): Boolean = retry {
    logger.info(s"Pushing to 's3://$bucketName/$key'")
    val req = PutObjectRequest.builder().bucket(bucketName).key(sanitizeKey(key))
    length.foreach(req.contentLength(_))

    Using(getTransferManager) { transfer =>
      transfer
        .upload(
          model.UploadRequest
            .builder()
            .putObjectRequest(req.build())
            .requestBody(AsyncRequestBody.fromInputStream { b =>
              b.inputStream(inputStream)
              length.foreach(b.contentLength(_))
              b.executor(defaultExecutor)
            })
            .build()
        )
        .completionFuture()
        .join()
    }.get
  }.isDefined

  /** Deletes the file in the location specified by `key` in the bucket.
    *
    * @param key
    *   the remote pathname for the file
    * @return
    *   true if the deletion was successful, false otherwise.
    */
  def delete(key: String): Boolean = retry {
    s3Client.deleteObject(_.bucket(bucketName).key(sanitizeKey(key))).join()
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
    def aux(): Boolean = {
      try {
        s3Client.headObject(_.bucket(bucketName).key(sanitizeKey(key))).join()
        true
      } catch {
        case ce: CompletionException if ce.getCause.isInstanceOf[NoSuchKeyException] =>
          false
      }
    }

    key.nonEmpty && aux()
  }.getOrElse(false)

  /** Checks if the location specified by `key` is a directory.
    *
    * @param key
    *   the remote pathname to the directory
    * @return
    *   true if the path is a directory, false otherwise.
    */
  def isDirectory(key: String): Boolean = retry {
    s3Client
      .listObjectsV2(_.bucket(bucketName).maxKeys(2).prefix(sanitizeKey(key)))
      .join()
      .contents
      .asScala
  }.exists(_.exists(_.key.startsWith(key + "/")))

  /** Checks whether the bucket exists
    *
    * @return
    *   true if the bucket exists, false otherwise.
    */
  def bucketExists: Boolean = bucketExists(s3Client)

  /** Sets an access control list on a given Amazon S3 object.
    *
    * @param key
    *   the remote pathname for the file
    * @param acl
    *   the `CannedAccessControlList` to be applied to the Amazon S3 object
    */
  def setAcl(key: String, acl: ObjectCannedACL) = {
    logger.info(s"Setting 's3://$bucketName/$key' permissions to '$acl'")
    s3Client.putObjectAcl(_.bucket(bucketName).key(sanitizeKey(key)).acl(acl)).join()
  }

  /** Creates an empty directory at the given `key` location
    *
    * @param key
    *   the remote pathname to the directory
    * @return
    *   true if the directory was created successfully, false otherwise.
    */
  def createDirectory(key: String): Boolean = retry {
    logger.info(s"Creating directory in 's3://$bucketName/$key'")
    val emptyContent = new ByteArrayInputStream(Array[Byte]())

    s3Client
      .putObject(
        b => { b.bucket(bucketName).key(sanitizeKey(key) + "/"); () },
        AsyncRequestBody.fromInputStream(emptyContent, 0, defaultExecutor)
      )
      .join()
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

    s3Client.copyObject(
      _.sourceBucket(bucketName)
        .sourceKey(sanitizedKey)
        .destinationBucket(bucketName)
        .destinationKey(mainKey + "/backup/" + name.substring(mainKey.length + 1) + extension)
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
    logger.info(s"Pulling 's3://$bucketName/$key' to '$destination'")

    val req = GetObjectRequest.builder().bucket(bucketName).key(sanitizeKey(key)).build()
    val destinationFile = new File(destination).getCanonicalFile
    destinationFile.getParentFile.mkdirs()
    s3Client.getObject(req, destinationFile.toPath()).join()
    logger.info(s"Downloaded 's3://$bucketName/$key' to '$destination'. Closing files.")
  }.isDefined

  def stream(key: String, offset: Long = 0L): InputStream = {
    logger.info(s"Streaming 's3://$bucketName/$key' starting at $offset")
    val req = GetObjectRequest.builder.bucket(bucketName).key(sanitizeKey(key))

    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Range
    if (offset > 0) req.range(s"bytes=$offset-")

    val fut: CompletableFuture[ResponseInputStream[GetObjectResponse]] =
      s3Client.getObject(req.build(), AsyncResponseTransformer.toBlockingInputStream)

    val stream = fut.join()
    stream
  }

  private def log(isError: Boolean, message: String, cause: Throwable): Unit =
    if (isError) logger.error(message, cause) else logger.warn(message, cause)

  private[this] def handler: PartialFunction[Throwable, Boolean] = {
    case ex: S3Exception =>
      ex.statusCode() match {
        case 404 =>
          logger.error("The specified file does not exist", ex); true // no need to retry
        case 403 =>
          logger.error("No permission to access the file", ex); true // no need to retry
        case _ =>
          logger.warn(
            s"""|S3 service error: ${ex.getMessage}. Extended request id: ${ex.requestId}
                      |Message: ${ex.getMessage}""".stripMargin,
            ex
          )
          false
      }
    case ex: SdkClientException =>
      log(!ex.retryable, s"Client Exception: ${ex.getMessage}", ex); !ex.retryable

    case ex: SdkException =>
      log(!ex.retryable, s"SDK Exception: ${ex.getMessage}", ex); !ex.retryable

    case ex: CompletionException =>
      logger.warn("Completion Exception", ex)
      handler(ex.getCause)

    case ex: Exception =>
      logger.warn("An error occurred", ex); false
  }

  private[this] def retry[T](f: => T, tries: Int = 3, sleepTime: Int = 5000): Option[T] =
    if (tries == 0) { logger.error("Max retries reached. Aborting S3 operation"); None }
    else
      Try(f) match {
        case Success(res)              => Some(res)
        case Failure(e) if !handler(e) =>
          if (tries > 1) {
            logger.warn(s"Error during S3 operation. Retrying in ${sleepTime}ms (${tries - 1} more times)")
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
