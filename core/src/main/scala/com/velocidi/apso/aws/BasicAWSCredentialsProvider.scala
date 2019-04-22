package com.velocidi.apso.aws

import com.amazonaws.auth.{ AWSCredentials, AWSCredentialsProvider, BasicAWSCredentials }

case class BasicAWSCredentialsProvider(accessKey: String, secretKey: String) extends AWSCredentialsProvider {
  def getCredentials: AWSCredentials = new BasicAWSCredentials(accessKey, secretKey)
  def refresh() {}
}
