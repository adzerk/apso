package com.velocidi.apso.aws

import com.amazonaws.auth.{ BasicAWSCredentials, AWSCredentials }

@deprecated("This will be removed in a future version", "2019/04/22")
case class SerializableAWSCredentials(accessKey: String, secretKey: String) extends AWSCredentials {
  def getAWSAccessKeyId: String = accessKey
  def getAWSSecretKey: String = secretKey
}

object SerializableAWSCredentials {
  def apply(basicCreds: BasicAWSCredentials): SerializableAWSCredentials = {
    SerializableAWSCredentials(basicCreds.getAWSAccessKeyId, basicCreds.getAWSSecretKey)
  }
}
