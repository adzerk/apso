package com.kevel.apso.gcp

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import java.nio.channels.Channels
import java.nio.file.Path

import scala.annotation.unused
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

import com.google.cloud.BaseServiceException
import com.google.cloud.storage.Storage.BlobListOption
import com.google.cloud.storage.{Blob, BlobId, BlobInfo, Storage, StorageException}
import com.typesafe.scalalogging.LazyLogging

final class GCSBucket(
    val bucketName: String,
    mkStorage: () => Storage
) extends Serializable
    with LazyLogging {
  @transient private[this] lazy val storage: Storage = mkStorage()

  private def blobId(key: String) = BlobId.of(bucketName, key)

  /** Returns size of the file in the location specified by `key` in the bucket. If the file doesn't exist the return
    * value is 0.
    *
    * @param key
    *   the remote pathname for the file
    * @return
    *   the size of the file in the location specified by `key` in the bucket if the exists, 0 otherwise.
    */
  def size(key: String): Long = retry {
    Option(storage.get(blobId(key))).map(_.getSize: Long).getOrElse(0L)
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
    Option(storage.get(blobId(key)))
      .flatMap(b => Option(b.getUpdateTimeOffsetDateTime.toInstant.toEpochMilli))
      .getOrElse(0L)
  }.getOrElse(0L)

  /** Returns a list of objects in a bucket matching a given prefix.
    *
    * NOTE: We cannot rely on the `BlobInfo.isDirectory` method to filter out since we are not using the
    * `BlobListOption.currentDirectory` option.
    *
    * @param prefix
    *   the prefix to match
    * @return
    *   a list of objects in a bucket matching a given prefix.
    */
  def getObjectsWithMatchingPrefix(prefix: String, includeDirectories: Boolean = false): Iterator[Blob] = retry {
    logger.info(s"Finding files matching prefix '$prefix'...")

    storage
      .list(bucketName, BlobListOption.prefix(prefix))
      .iterateAll()
      .asScala
      .iterator
      .filter(b => includeDirectories || !b.getName.endsWith("/"))
  }.getOrElse(Iterator.empty)

  // FIXME: If the root directory/prefix was created by the `createDirectory` method (where we create an object with 0 bytes)
  //        that root directory will be present in the results. Evaluate if we should filter it out since it does not
  //        represent a file/folder inside the requested prefix.
  /** Assuming prefix points to a folder, returns all filenames and optional object blobs immediately below that folder.
    * Only actual files will have a Blob, folders won't.
    *
    * @param prefix
    *   the prefix in which to list files
    * @return
    *   a list of filenames and optional object blobs in a bucket directly "below" the provided prefix.
    */
  def getFilesInFolder(prefix: String): Iterator[(String, Option[Blob])] = retry {
    logger.info(s"Finding files in folder '$prefix'...")

    val normalized = if (prefix.endsWith("/")) prefix else if (prefix.isEmpty) "" else prefix + "/"
    val pages = storage.list(
      bucketName,
      BlobListOption.prefix(normalized),
      BlobListOption.currentDirectory()
    )
    pages.iterateAll().asScala.iterator.map { blob =>
      blob.getName -> Option(blob)
    }
  }.getOrElse(Iterator.empty)

  /** Pushes a given local `File` to the location specified by `key` in the bucket.
    *
    * @param key
    *   the remote pathname for the file
    * @param file
    *   the local `File` to push
    * @return
    *   true if the push was successful, false otherwise.
    */
  def push(key: String, file: File): Boolean = {
    logger.info(s"Pushing file '${file.getPath}' to 'gs://$bucketName/$key'")
    push(key, new FileInputStream(file), None)
  }

  /** Pushes a given `InputStream` to the location specified by `key` in the bucket.
    *
    * NOTE: We are ignoring the length parameter for now.
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
  def push(key: String, inputStream: InputStream, @unused length: Option[Long]): Boolean = {
    logger.info(s"Pushing to 'gs://$bucketName/$key'")
    val info = BlobInfo.newBuilder(blobId(key)).build()
    storage.createFrom(info, new BufferedInputStream(inputStream)).exists()
  }

  /** Deletes the file in the location specified by `key` in the bucket.
    *
    * @param key
    *   the remote pathname for the file
    * @return
    *   true if the deletion was successful, false otherwise.
    */
  def delete(key: String): Boolean = retry {
    storage.delete(blobId(key))
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
    val b = storage.get(blobId(key))
    b != null && (b.exists() || b.isDirectory)
  }.getOrElse(false)

  /** Checks if the location specified by `key` is a directory.
    *
    * @param key
    *   the remote pathname to the directory
    * @return
    *   true if the path is a directory, false otherwise.
    */
  def isDirectory(key: String): Boolean = retry {
    val prefix = if (key.endsWith("/")) key else key + "/"
    storage
      .list(bucketName, BlobListOption.prefix(prefix), BlobListOption.pageSize(1))
      .iterateAll()
      .iterator()
      .hasNext
  }.getOrElse(false)

  /** Checks whether the bucket exists
    *
    * @return
    *   true if the bucket exists, false otherwise.
    */
  def bucketExists: Boolean = storage.get(bucketName) != null

  /** Creates an empty directory at the given `key` location
    *
    * @param key
    *   the remote pathname to the directory
    * @return
    *   true if the directory was created successfully, false otherwise.
    */
  def createDirectory(key: String): Boolean = retry {
    val dirKey = if (key.endsWith("/")) key else key + "/"
    logger.info(s"Creating directory in 'gs://$bucketName/$dirKey'")

    val info = BlobInfo.newBuilder(blobId(dirKey)).build()
    storage.create(info, Array.emptyByteArray)
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
    logger.info(s"Pulling 'gs://$bucketName/$key' to '$destination'")
    storage.downloadTo(blobId(key), Path.of(destination))
    logger.info(s"Downloaded 'gs://$bucketName/$key' to '$destination'. Closing files.")
  }.isDefined

  def stream(key: String, offset: Long = 0L): InputStream = {
    logger.info(s"Streaming 'gs://$bucketName/$key' starting at $offset")
    val reader = storage.reader(blobId(key))
    if (offset > 0L) reader.seek(offset)
    Channels.newInputStream(reader)
  }

  private[this] def handler: PartialFunction[Throwable, Boolean] = {
    case ex: StorageException =>
      ex.getCode match {
        case 404 =>
          logger.error("The specified file does not exist", ex); true // no need to retry
        case 403 =>
          logger.error("No permission to access the file", ex); true // no need to retry
        case _ =>
          logger.warn(
            s"""|GCS service error: ${ex.getMessage}.
                |Additional details: ${ex.getDebugInfo}""".stripMargin,
            ex
          )
          false
      }

    case ex: BaseServiceException =>
      logger.warn("An error occurred", ex); ex.isRetryable
  }

  private[this] def retry[T](f: => T, tries: Int = 3, sleepTime: Int = 5000): Option[T] =
    if (tries == 0) { logger.error("Max retries reached. Aborting GCS operation"); None }
    else
      Try(f) match {
        case Success(res)              => Some(res)
        case Failure(e) if !handler(e) =>
          if (tries > 1) {
            logger.warn(s"Error during GCS operation. Retrying in ${sleepTime}ms (${tries - 1} more times)")
            Thread.sleep(sleepTime)
          }
          retry(f, tries - 1, sleepTime)

        case _ => None
      }
}
