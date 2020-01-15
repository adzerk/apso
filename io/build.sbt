import Dependencies._

libraryDependencies ++= Seq(
  AwsJavaSdkS3   % Provided,
  Log4jApiScala,
  ScalaPool,
  SshJ,
  TypesafeConfig % Provided,
  Specs2Core     % Test)
