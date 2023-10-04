import Dependencies._

libraryDependencies ++= Seq(
  ScalaLogging,
  AwsJavaSdkS3,
  AwsJavaSdkCore,
  ScalaCollectionCompat,
  ScalaLogging,
  TypesafeConfig,
  Specs2Core % Test
)
