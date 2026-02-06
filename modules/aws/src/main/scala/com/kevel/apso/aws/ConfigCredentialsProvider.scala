package com.kevel.apso.aws

import com.typesafe.config.{Config, ConfigFactory}
import software.amazon.awssdk.auth.credentials.*

/** AWS credentials provider that retrieves credentials from a typesafe configuration.
  * @param config
  *   the typesafe configuration
  * @param accessKeyPath
  *   the path in the configuration that contains the access key
  * @param secretKeyPath
  *   the path in the configuration that contains the secret key
  */
case class ConfigCredentialsProvider(
    config: Config = ConfigFactory.load(),
    accessKeyPath: String = "aws.access-key",
    secretKeyPath: String = "aws.secret-key"
) extends AwsCredentialsProvider {

  def resolveCredentials(): AwsCredentials =
    AwsBasicCredentials.create(config.getString(accessKeyPath), config.getString(secretKeyPath))

}
