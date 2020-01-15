import Dependencies._

libraryDependencies ++= Seq(
  AwsJavaSdkS3   % Provided,
  Log4jApiScala,
  TypesafeConfig % Provided,
  Specs2Core     % Test)
