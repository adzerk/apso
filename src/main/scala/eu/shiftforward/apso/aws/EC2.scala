package eu.shiftforward.apso.aws

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.ec2.{AmazonEC2, AmazonEC2Client}
import com.amazonaws.services.ec2.model._
import eu.shiftforward.apso.Logging
import scala.collection.convert.WrapAsScala._

/**
 * A representation of Amazon's EC2 service. This class wraps an
 * [[com.amazonaws.services.ec2.AmazonEC2]] instance and provides a higher level interface for
 * querying the currently running instances.
 * @param credentials optional AWS credentials to use. If the parameter is not supplied, they will
 *                    be retrieved from the [[eu.shiftforward.apso.aws.CredentialStore]].
 */
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
    new DescribeInstancesRequest().withInstanceIds(id)).getReservations.headOption.
    flatMap(_.getInstances.headOption)

  /**
   * Returns all the currently running instances in EC2.
   * @return a sequence containing all the currently running instances in EC2.
   */
  def instances(): Seq[Instance] = {
    val instanceData = client.describeInstances()
    instanceData.getReservations.flatMap(_.getInstances)
  }
}

