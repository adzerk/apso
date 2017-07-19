package eu.shiftforward.apso.aws

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3Client }
import com.amazonaws.services.s3.model._
import eu.shiftforward.apso.Logging
import scala.collection.JavaConverters._

/**
 * A representation of Amazon's S3 service. This class wraps an
 * [[com.amazonaws.services.s3.AmazonS3]] instance and provides a higher level interface for
 * querying the information about the buckets and their objects.
 * @param credentials optional AWS credentials to use. If the parameter is not supplied, they will
 *                    be retrieved from the [[eu.shiftforward.apso.aws.CredentialStore]].
 */
@deprecated("This will be removed in a future version", "2017/07/13")
class S3(credentials: AWSCredentials = CredentialStore.getCredentials) extends Logging {

  /**
   * The underlying [[com.amazonaws.services.s3.AmazonS3]] instance.
   */
  val client: AmazonS3 = new AmazonS3Client(credentials)

  /**
   * Returns a list of all the buckets listable with this object's credentials.
   * @return a list of all the buckets listable with this object's credentials.
   */
  def buckets: Seq[Bucket] = client.listBuckets.asScala

  /**
   * Returns the representation of an object in a bucket, if it exists.
   * @param bucketName the name of the bucket
   * @param objName the name of the object
   * @return the object wrapped in a `Some` if that object exists, `None` otherwise.
   */
  def apply(bucketName: String, objName: String): Option[S3Object] = try {
    Some(client.getObject(bucketName, objName))
  } catch {
    case e: AmazonS3Exception => None
  }

  /**
   * Returns the metadata of an object in a bucket, if it exists.
   * @param bucketName the name of the bucket
   * @param objName the name of the object
   * @return the object metadata wrapped in a `Some` if that object exists, `None` otherwise.
   */
  def metadata(bucketName: String, objName: String): Option[ObjectMetadata] = try {
    Some(client.getObjectMetadata(bucketName, objName))
  } catch {
    case e: AmazonS3Exception => None
  }

  /**
   * Returns the tags associated with a bucket.
   * @param bucket the bucket object
   * @return a map containing the tags associated with the given bucket. If the bucket doesn't
   *         exist, an empty map is returned.
   */
  @inline def bucketTags(bucket: Bucket): Map[String, String] = bucketTags(bucket.getName)

  /**
   * Returns the tags associated with a bucket.
   * @param bucketName the name of the bucket
   * @return a map containing the tags associated with the given bucket. If the bucket doesn't
   *         exist, an empty map is returned.
   */
  def bucketTags(bucketName: String): Map[String, String] = {
    Option(client.getBucketTaggingConfiguration(bucketName))
      .map(_.getTagSet.getAllTags.asScala.toMap).getOrElse(Map())
  }

  /**
   * Returns the first object in a bucket, optionally with a given prefix and after a given marker,
   * if it exists.
   * @param bucketName the name of the bucket
   * @param prefix the prefix of the object to return, or `null` for no prefix
   * @param marker the marker from which the search is to be done, or `null` to search from the
   *               beginning of the bucket
   * @return the first object in a bucket with the given prefix after the given marker wrapped in a
   *         `Some` if such an object exists, `None` otherwise.
   */
  @inline def headObject(bucketName: String, prefix: String = null, marker: String = null): Option[S3ObjectSummary] =
    objects(bucketName, prefix, marker).toStream.headOption

  /**
   * Returns the objects in a bucket, optionally with a given prefix and after a given marker.
   * @param bucketName the name of the bucket
   * @param prefix the prefix of the objects to return, or `null` for no prefix
   * @param marker the marker from which the search is to be done, or `null` to search from the
   *               beginning of the bucket
   * @param maxKeys the maximum number of objects to return. As S3 can limit the number of objects
   *                returned in a single request, there is no guarantee that `maxKeys` objects will
   *                be returned, even if there is more than `maxKeys` objects in S3 with the
   *                specified constraints.
   * @return a list with at most `maxKeys` objects in a bucket with the given prefix after the given
   *         marker.
   */
  def objects(
    bucketName: String,
    prefix: String = null,
    marker: String = null,
    maxKeys: Int = 1): Iterator[S3ObjectSummary] = {

    def loop(last: String, max: Int): Iterator[S3ObjectSummary] = {
      val req = new ListObjectsRequest(bucketName, prefix, last, null, max)
      val objs = client.listObjects(req).getObjectSummaries.asScala

      if (objs.length == max || objs.length < 1000) objs.toIterator
      else objs.toIterator ++ loop(objs.last.getKey, max - 1000)
    }
    loop(marker, maxKeys)
  }
}
