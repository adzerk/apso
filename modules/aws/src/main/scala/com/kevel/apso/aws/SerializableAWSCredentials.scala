package com.kevel.apso.aws

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, AwsCredentials}

case class SerializableAWSCredentials(accessKey: String, secretKey: String) extends AwsCredentials {
  override def accessKeyId(): String = accessKey
  override def secretAccessKey(): String = secretKey
}

object SerializableAWSCredentials {
  def apply(basicCreds: AwsBasicCredentials): SerializableAWSCredentials = {
    SerializableAWSCredentials(basicCreds.accessKeyId, basicCreds.secretAccessKey)
  }
}
