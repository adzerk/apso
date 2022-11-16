package com.velocidi.apso.aws

import software.amazon.awssdk.auth.credentials._

/** An object that serves as an endpoint for the retrieval of AWS credentials from available configurations. In
  * particular, it extends the chain in the `DefaultAWSCredentialsProviderChain` with the retrieval of AWS credentials
  * through the default typesafe configuration file (typically application.conf).
  */
object CredentialStore {
  val providerChain = () =>
    AwsCredentialsProviderChain.of(new ConfigCredentialsProvider, DefaultCredentialsProvider.create())
}
