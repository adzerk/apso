import Dependencies._

libraryDependencies ++= Seq(
  AwsJavaSdkS3,
  AwsJavaSdkCore,
  ScalaLogging,
  TypesafeConfig,
  Specs2Core     % Test)
