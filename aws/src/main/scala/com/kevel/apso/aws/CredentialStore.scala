package com.kevel.apso.aws

import com.amazonaws.auth._

/** An object that serves as an endpoint for the retrieval of AWS credentials from available configurations. In
  * particular, it extends the chain in the `DefaultAWSCredentialsProviderChain` with the retrieval of AWS credentials
  * through the default typesafe configuration file (typically application.conf).
  */
object CredentialStore
    extends AWSCredentialsProviderChain(new ConfigCredentialsProvider, new DefaultAWSCredentialsProviderChain)
