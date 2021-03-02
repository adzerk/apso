import Dependencies._

libraryDependencies ++= Seq(
  AwsJavaSdkS3,
  AwsJavaSdkCore,
  ScalaLogging,
  ScalaPool,
  SshJ,
  TypesafeConfig,
  Specs2Core     % Test)
