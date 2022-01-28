import Dependencies._

libraryDependencies ++= Seq(
  AwsJavaSdkS3,
  AwsJavaSdkCore,
  ScalaCollectionCompat,
  ScalaLogging,
  TypesafeConfig,
  Specs2Core % Test
)
