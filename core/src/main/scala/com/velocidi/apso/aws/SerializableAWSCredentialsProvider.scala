package com.velocidi.apso.aws

import java.util.UUID

import com.amazonaws.auth.{ AWSCredentials, AWSCredentialsProvider, BasicAWSCredentials, STSAssumeRoleSessionCredentialsProvider }
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder

case class SerializableAWSCredentialsProvider(accessKey: String, secretKey: String, roleArn: Option[String]) extends AWSCredentialsProvider {
  lazy val innerProvider: AWSCredentialsProvider = {
    lazy val baseCredentialsProvider: AWSCredentialsProvider =
      StaticAWSCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey))

    roleArn.fold(baseCredentialsProvider) { role =>
      new STSAssumeRoleSessionCredentialsProvider.Builder(role, UUID.randomUUID().toString)
        .withStsClient(AWSSecurityTokenServiceClientBuilder.standard().withCredentials(baseCredentialsProvider).build())
        .build()
    }
  }

  def getCredentials: AWSCredentials = innerProvider.getCredentials()
  def refresh(): Unit = innerProvider.refresh()
}
