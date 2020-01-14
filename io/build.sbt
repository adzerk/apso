import Dependencies._

libraryDependencies ++= Seq(
  AwsJavaSdkS3   % Provided,
  ScalaLogging,
  ScalaPool,
  SshJ,
  TypesafeConfig % Provided,
  Specs2Core     % Test)
