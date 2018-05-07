package eu.shiftforward.apso.aws

import java.io._
import java.util.concurrent.{ Executors, ThreadFactory }

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

import com.amazonaws.{ AmazonClientException, AmazonServiceException, ClientConfiguration }
import com.amazonaws.auth._
import com.amazonaws.client.builder.ExecutorFactory
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3ClientBuilder }
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.transfer.{ TransferManager, TransferManagerBuilder }
import com.typesafe.config.ConfigFactory

import eu.shiftforward.apso.aws.S3Bucket.S3ObjectDownloader
import eu.shiftforward.apso.{ Logging, TryWith }

/**
 * A representation of an Amazon's S3 bucket. This class wraps an
 * [[com.amazonaws.services.s3.AmazonS3Client]] and provides a higher level interface for pushing
 * and pulling files to and from a bucket.
 *
 * @param bucketName the name of the bucket
 * @param credentialsProvider optional AWS credentials provider to use (since AWSCredentials are not serializable).
 *                    If the parameter is not supplied, they will be retrieved from the
 *                    [[eu.shiftforward.apso.aws.CredentialStore]].
 */
class S3Bucket(
    val bucketName: String,
    private val credentialsProvider: () => AWSCredentialsProvider = () => CredentialStore)
  extends Logging with Serializable {

  private[this] lazy val config = ConfigFactory.load()

  private[this] lazy val configPrefix = "aws.s3"
  private[this] lazy val region = Try(config.getString(configPrefix + ".region"))

  @transient private[this] var _s3: AmazonS3 = _

  private[this] def s3 = {
    if (_s3 == null) {
      val defaultConfig = new ClientConfiguration()
        .withTcpKeepAlive(true)

      _s3 = AmazonS3ClientBuilder.standard
        .withCredentials(credentialsProvider())
        .withClientConfiguration(defaultConfig)
        .withForceGlobalBucketAccessEnabled(true)
        .build()

      if (!_s3.doesBucketExistV2(bucketName)) {
        _s3.createBucket(
          new CreateBucketRequest(
            bucketName,
            region.map(Region.fromValue).getOrElse(Region.US_Standard)))
      }
    }
    _s3
  }

  @transient private[this] var _transferManager: TransferManager = _

  private[this] def transferManager = {
    if (_transferManager == null) {
      // This is the default thread pool used by the `TransferManager`. I had to replicate it here
      // to make sure that threads are daemonized. This could be problematic, e.g. shutting down the
      // JVM before a transfer has finished, although this won't happen in our use case since we
      // always wait for transfers to finish.
      // (This code is copy-pasted from the AWS SDK with the addition of the `setDaemon` call).
      val executor = Executors.newFixedThreadPool(10, new ThreadFactory() {
        var threadCount = 1
        def newThread(r: Runnable) = {
          val thread = new Thread(r)
          thread.setDaemon(true)
          thread.setName(s"s3-transfer-manager-worker-$threadCount")
          threadCount += 1
          thread
        }
      })

      val executorFactory = new ExecutorFactory { def newExecutor() = executor }
      _transferManager = TransferManagerBuilder.standard.withS3Client(s3).withExecutorFactory(executorFactory).build()
    }
    _transferManager
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

  /**
   * Returns size of the file in the location specified by `key` in the bucket. If the file doesn't
   * exist the return value is 0.
   *
   * @param key the remote pathname for the file
   * @return the size of the file in the location specified by `key` in the bucket if the exists, 0 otherwise.
   */
  def size(key: String): Long = retry {
    s3.getObjectMetadata(bucketName, key).getContentLength
  }.getOrElse(0)

  /**
   * Returns a list of objects in a bucket matching a given prefix.
   *
   * @param prefix the prefix to match
   * @return a list of objects in a bucket matching a given prefix.
   */
  def getObjectsWithMatchingPrefix(prefix: String, includeDirectories: Boolean = false): Iterator[S3ObjectSummary] = {
    log.info(s"Finding files matching prefix '$prefix'...")

    val listings = Iterator.iterate(s3.listObjects(bucketName, sanitizeKey(prefix))) { listing =>
      if (listing.isTruncated) {
        log.debug("Asking for another batch of objects...")
        s3.listNextBatchOfObjects(listing)
      } else null
    }

    val objects = listings.takeWhile(_ != null).flatMap(_.getObjectSummaries.asScala)
    if (includeDirectories) objects else objects.filterNot(_.getKey.endsWith("/"))
  }

  /**
   * Returns a list of filenames and directories in a bucket matching a given prefix.
   *
   * @param prefix the prefix to match
   * @return a list of filenames in a bucket matching a given prefix.
   */
  def getFilesWithMatchingPrefix(prefix: String, includeDirectories: Boolean = false): Iterator[String] =
    getObjectsWithMatchingPrefix(prefix, includeDirectories).map(_.getKey)

  /**
   * Pushes a given local `File` to the location specified by `key` in the bucket.
   *
   * @param key the remote pathname for the file
   * @param file the local `File` to push
   * @return true if the push was successful, false otherwise.
   */
  def push(key: String, file: File): Boolean = retry {
    log.info(s"Pushing file '${file.getPath}' to 's3://$bucketName/$key'")
    transferManager
      .upload(new PutObjectRequest(bucketName, sanitizeKey(key), file))
      .waitForUploadResult()
  }.isDefined

  /**
   * Pushes a given `InputStream` to the location specified by `key` in the bucket.
   *
   * @param key the remote pathname for the file
   * @param inputStream the `InputStream` to push
   * @param length the content lenght (setting this to `None` can impact performance
   * @return true if the push was successful, false otherwise.
   */
  def push(key: String, inputStream: InputStream, length: Option[Long]): Boolean = retry {
    log.info(s"Pushing to 's3://$bucketName/$key'")
    val metadata = new ObjectMetadata()
    length.foreach(metadata.setContentLength)
    transferManager
      .upload(new PutObjectRequest(bucketName, sanitizeKey(key), inputStream, metadata))
      .waitForUploadResult()
  }.isDefined

  /**
   * Deletes the file in the location specified by `key` in the bucket.
   *
   * @param key the remote pathname for the file
   * @return true if the deletion was successful, false otherwise.
   */
  def delete(key: String): Boolean = retry {
    s3.deleteObject(bucketName, sanitizeKey(key))
  }.isDefined

  /**
   * Checks if the file in the location specified by `key` in the bucket exists.
   *
   * @param key the remote pathname for the file
   * @return true if the file exists, false otherwise.
   */
  def exists(key: String): Boolean = retry {
    s3.getObjectMetadata(bucketName, key)
  }.isDefined

  /**
   * Checks if the location specified by `key` is a directory.
   *
   * @param key the remote pathname to the directory
   * @return true if the path is a directory, false otherwise.
   */
  def isDirectory(key: String): Boolean = retry {
    s3.listObjects(
      new ListObjectsRequest()
        .withBucketName(bucketName)
        .withMaxKeys(2)
        .withPrefix(key)).getObjectSummaries.asScala
  }.exists { _.exists(_.getKey.startsWith(key + "/")) }

  /**
   * Sets an access control list on a given Amazon S3 object.
   *
   * @param key the remote pathname for the file
   * @param acl the `CannedAccessControlList` to be applied to the Amazon S3 object
   */
  def setAcl(key: String, acl: CannedAccessControlList) {
    log.info(s"Setting 's3://$bucketName/$key' permissions to '$acl'")
    s3.setObjectAcl(bucketName, key, acl)
  }

  /**
   * Creates an empty directory at the given `key` location
   *
   * @param key the remote pathname to the directory
   * @return  true if the directory was created successfully, false otherwise.
   */
  def createDirectory(key: String): Boolean = retry {
    log.info(s"Creating directory in 's3://$bucketName/$key'")

    val emptyContent = new ByteArrayInputStream(Array[Byte]())
    val metadata = new ObjectMetadata()
    metadata.setContentLength(0)

    s3.putObject(new PutObjectRequest(bucketName, sanitizeKey(key) + "/", emptyContent, metadata))
  }.isDefined

  /**
   * Backups a remote file with the given `key`. A backup consists in copying the supplied file to a backup folder under
   * the same bucket and folder the file is currently in.
   *
   * @param key the remote pathname to backup
   * @return true if the backup was successful, false otherwise.
   */
  def backup(key: String): Boolean = retry {
    val sanitizedKey = sanitizeKey(key)
    val (mainKey, name, extension) = splitKey(sanitizedKey)

    s3.copyObject(new CopyObjectRequest(
      bucketName,
      sanitizedKey,
      bucketName,
      mainKey + "/backup/" + name.substring(mainKey.length + 1) + extension))
  }.isDefined

  /**
   * Pulls a remote file with the given `key`, to the local storage in the pathname provided by `destination`.
   *
   * @param key the remote pathname to pull from
   * @param destination the local pathname to pull to
   * @return true if the pull was successful, false otherwise
   */
  def pull(key: String, destination: String): Boolean = retry {
    log.info(s"Pulling 's3://$bucketName/$key' to '$destination'")
    TryWith(new S3ObjectDownloader(s3, bucketName, sanitizeKey(key), destination))(_.download()).get
    log.info(s"Downloaded 's3://$bucketName/$key' to '$destination'. Closing files.")
  }.isDefined

  def stream(key: String, offset: Long = 0L): InputStream = {
    log.info(s"Streaming 's3://$bucketName/$key' starting at $offset")
    val req =
      if (offset > 0) new GetObjectRequest(bucketName, sanitizeKey(key)).withRange(offset)
      else new GetObjectRequest(bucketName, sanitizeKey(key))
    s3.getObject(req).getObjectContent
  }

  private[this] def handler: PartialFunction[Throwable, Boolean] = {
    case ex: AmazonS3Exception => ex.getStatusCode match {
      case 404 =>
        log.error("The specified file does not exist", ex); true // no need to retry
      case 403 =>
        log.error("No permission to access the file", ex); true // no need to retry
      case _ =>
        log.error(s"""|S3 service error: ${ex.getMessage}. Extended request id: ${ex.getExtendedRequestId}
                      |Additional details: ${ex.getAdditionalDetails}""".stripMargin, ex)
        false
    }

    case ex: AmazonServiceException =>
      log.error(s"Service error: ${ex.getMessage}", ex); ex.isRetryable

    case ex: AmazonClientException if ex.getMessage ==
      "Unable to load AWS credentials from any provider in the chain" =>
      log.error("Unable to load AWS credentials", ex); true

    case ex: AmazonClientException =>
      log.error("Client error pulling file", ex); ex.isRetryable

    case ex: Exception =>
      log.error("An error occurred", ex); false
  }

  private[this] def retry[T](f: => T, tries: Int = 3, sleepTime: Int = 5000): Option[T] =
    if (tries == 0) { log.error("Max retries reached. Aborting S3 operation"); None }
    else Try(f) match {
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
    case _ => false
  }
}

object S3Bucket {
  private class S3ObjectDownloader(s3: AmazonS3, bucketName: String, key: String, fileDestination: String) extends AutoCloseable {
    private[this] val s3Object: S3Object = s3.getObject(new GetObjectRequest(bucketName, key))
    private[this] val inputStream: S3ObjectInputStream = s3Object.getObjectContent
    private[this] val outputStream: BufferedOutputStream = {
      val f = new File(fileDestination).getCanonicalFile
      f.getParentFile.mkdirs()
      new BufferedOutputStream(new FileOutputStream(f))
    }

    def close(): Unit = {
      try inputStream.close() catch { case _: Throwable => }
      try s3Object.close() catch { case _: Throwable => }
      try outputStream.flush() catch { case _: Throwable => }
      try outputStream.close() catch { case _: Throwable => }
    }

    def download(): Unit = {
      var read = 0
      val bytes = new Array[Byte](1024)

      read = inputStream.read(bytes)
      while (read != -1) {
        outputStream.write(bytes, 0, read)
        read = inputStream.read(bytes)
      }
    }

  }
}
