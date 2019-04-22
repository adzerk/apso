package com.velocidi.apso.aws

import com.amazonaws.auth.{ AWSCredentials, AWSCredentialsProvider, BasicAWSCredentials }

case class StaticAWSCredentialsProvider(credentials: BasicAWSCredentials) extends AWSCredentialsProvider {
  def getCredentials: AWSCredentials = credentials
  def refresh() {}
}
