package eu.shiftforward.apso.aws

import com.amazonaws.auth.{ BasicAWSCredentials, AWSCredentials }

case class SerializableAWSCredentials(accessKey: String, secretKey: String) extends AWSCredentials {
  def getAWSAccessKeyId: String = accessKey
  def getAWSSecretKey: String = secretKey
}

object SerializableAWSCredentials {
  def apply(basicCreds: BasicAWSCredentials): SerializableAWSCredentials = {
    SerializableAWSCredentials(basicCreds.getAWSAccessKeyId, basicCreds.getAWSSecretKey)
  }
}
