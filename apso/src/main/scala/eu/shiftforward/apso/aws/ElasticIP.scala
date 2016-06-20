package eu.shiftforward.apso.aws

import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.AssociateAddressRequest
import com.amazonaws.auth.AWSCredentials

/**
 * Representation of an AWS Elastic IP address.
 * @param ip the IPv4 address
 */
class ElasticIP(ip: String, credentials: AWSCredentials = CredentialStore.getCredentials) {

  private[this] lazy val ec2 = new AmazonEC2Client(credentials)

  /**
   * Associate this address with the given running EC2 instance.
   * @param instanceId the EC2 instance id
   */
  def associateTo(instanceId: String) {
    ec2.associateAddress(new AssociateAddressRequest(instanceId, ip))
  }
}
