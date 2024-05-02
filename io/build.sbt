import Dependencies._

libraryDependencies ++= Seq(
  AwsJavaSdkCore,
  AwsJavaSdkS3,
  // FIX: Explicitly override transient bouncy castle versions from `sshj`: https://security.snyk.io/vuln/SNYK-JAVA-ORGBOUNCYCASTLE-6612984
  // Remove once com.hierynomus:sshj releases a version with https://github.com/hierynomus/sshj/pull/938
  BouncyCastlePkix,
  BouncyCastleProvider,
  ScalaCollectionCompat,
  ScalaLogging,
  ScalaPool,
  SshJ,
  TypesafeConfig,
  Specs2Core % Test
)
