import Dependencies._

libraryDependencies ++= Seq(
  ScalaTestCore,
  Specs2Common  % Provided,
  Specs2Core    % Provided,
  Specs2Matcher % Provided
)
