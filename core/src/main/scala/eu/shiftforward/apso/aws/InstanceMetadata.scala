package eu.shiftforward.apso.aws

import scala.language.postfixOps
import scala.sys.process._

/**
 * Utilities for obtaining metadata about the EC2 instance this process is running on. The methods
 * in this object are not expected to work if the JVM is not running on an EC2 instance.
 */
@deprecated("This will be removed in a future version", "2017/07/13")
object InstanceMetadata {

  /**
   * Retrieves the given metadata about this EC2 instance from the instance metadata server.
   * @param key the metadata key
   * @return the requested metadata about this EC2 instance.
   */
  def metadata(key: String): String =
    (s"/usr/bin/curl -s http://169.254.169.254/latest/meta-data/$key"!!).trim

  /**
   * Convenience method that returns the instance ID of this EC2 instance.
   * @return the instance ID of this EC2 instance.
   */
  def instanceId = metadata("instance-id")
}
