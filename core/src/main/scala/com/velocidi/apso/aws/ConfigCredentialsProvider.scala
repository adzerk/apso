package com.velocidi.apso.aws

import java.util.UUID

import com.amazonaws.auth._
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
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

  lazy val basicCredentialsProvider =
    BasicAWSCredentialsProvider(config.getString(accessKeyPath), config.getString(secretKeyPath))
  lazy val provider: AWSCredentialsProvider = if (config.hasPath(roleArnPath))
    new STSAssumeRoleSessionCredentialsProvider.Builder(config.getString(roleArnPath), UUID.randomUUID().toString)
      .withStsClient(AWSSecurityTokenServiceClientBuilder.standard().withCredentials(basicCredentialsProvider).build())
      .build()
  else
    basicCredentialsProvider

  def getCredentials: AWSCredentials =
    provider.getCredentials()

  def refresh(): Unit = provider.refresh()
}
