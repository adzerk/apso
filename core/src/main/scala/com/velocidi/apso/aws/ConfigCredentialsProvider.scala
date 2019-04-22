package com.velocidi.apso.aws

import com.amazonaws.auth._
import com.typesafe.config.{ Config, ConfigFactory }

/**
 * AWS credentials provider that retrieves credentials from a typesafe configuration.
 * @param config the typesafe configuration
 * @param accessKeyPath the path in the configuration that contains the access key
 * @param secretKeyPath the path in the configuration that contains the secret key
 */
case class ConfigCredentialsProvider(
    config: Config = ConfigFactory.load(),
    accessKeyPath: String = "aws.access-key",
    secretKeyPath: String = "aws.secret-key",
    roleArnPath: String = "aws.role-arn")
  extends AWSCredentialsProvider {

  lazy val provider = SerializableAWSCredentialsProvider(
    config.getString(accessKeyPath),
    config.getString(secretKeyPath),
    if (config.hasPath(roleArnPath)) Some(config.getString(roleArnPath)) else None)

  def getCredentials: AWSCredentials =
    provider.getCredentials()

  def refresh(): Unit = provider.refresh()
}
