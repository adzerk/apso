import Dependencies._

libraryDependencies ++= Seq(
  AwsJavaSdkS3,
  AwsJavaSdkCore,
  ScalaCollectionCompat,
  ScalaLogging,
  ScalaPool,
  SshJ,
  TypesafeConfig,
  Specs2Core % Test
)
