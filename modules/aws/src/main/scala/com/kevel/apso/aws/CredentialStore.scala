package com.kevel.apso.aws

import software.amazon.awssdk.auth.credentials.*

object CredentialStore {

  /** Serves as an endpoint for the retrieval of AWS credentials from available configurations. In particular, it
    * extends the chain in the `DefaultCredentialsProvider` with the retrieval of AWS credentials through the default
    * typesafe configuration file (typically application.conf).
    */
  final val credentialsProvider = AwsCredentialsProviderChain
    .builder()
    .addCredentialsProvider(new ConfigCredentialsProvider())
    .addCredentialsProvider(DefaultCredentialsProvider.builder.build)
    .build
}
