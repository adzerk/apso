package eu.shiftforward.apso.aws

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.ec2.{ AmazonEC2, AmazonEC2Client }
import com.amazonaws.services.ec2.model._
import eu.shiftforward.apso.Logging
import scala.collection.JavaConverters._

/**
 * A representation of Amazon's EC2 service. This class wraps an
 * [[com.amazonaws.services.ec2.AmazonEC2]] instance and provides a higher level interface for
 * querying the currently running instances.
 * @param credentials optional AWS credentials to use. If the parameter is not supplied, they will
 *                    be retrieved from the [[eu.shiftforward.apso.aws.CredentialStore]].
 */
@deprecated("This will be removed in a future version", "2017/07/13")
class EC2(credentials: AWSCredentials = CredentialStore.getCredentials) extends Logging {

  /**
   * The underlying [[com.amazonaws.services.ec2.AmazonEC2]] instance.
   */
  val client: AmazonEC2 = new AmazonEC2Client(credentials)

  /**
   * Returns the information about the instance with a given id.
   * @param id the id of the instance whose data is to be retrieved
   * @return the information about the instance with the given id wrapped in a `Some` if such an
   *         instance exists, `None` otherwise.
   */
  def instance(id: String): Option[Instance] = client.describeInstances(
    new DescribeInstancesRequest().withInstanceIds(id)).getReservations.asScala.headOption.
    flatMap(_.getInstances.asScala.headOption)

  /**
   * Returns all the currently running instances in EC2.
   * @return a sequence containing all the currently running instances in EC2.
   */
  def instances(): Seq[Instance] = {
    val instanceData = client.describeInstances()
    instanceData.getReservations.asScala.flatMap(_.getInstances.asScala)
  }

  /**
   * Terminates an instance.
   * @param instance the instance to terminate
   */
  @inline def terminate(instance: Instance) {
    terminate(instance.getInstanceId)
  }

  /**
   * Terminates an instance.
   * @param instanceId the id of the instance to terminate
   */
  def terminate(instanceId: String) {
    client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceId))
  }
}

/**
 * Object providing extension methods for [[com.amazonaws.services.ec2.AmazonEC2]] related models.
 */
object EC2 {

  /**
   * Extension class for an [[com.amazonaws.services.ec2.model.Instance]].
   * @param instance the instance to which the extension methods are to be available
   */
  implicit class RichEC2Instance(val instance: Instance) extends AnyVal {

    /**
     * Returns the id of this instance.
     * @return the id of this instance.
     */
    def id = instance.getInstanceId

    /**
     * Returns the value of a tag.
     * @param key the key of the tag whose value is to be retrieved
     * @return the value associated with the given key wrapped in a `Some` if the tag exists, `None`
     *         otherwise.
     */
    def tagValue(key: String) = instance.getTags.asScala.find(_.getKey == key).map(_.getValue)
  }
}
