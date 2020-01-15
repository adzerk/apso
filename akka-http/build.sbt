import Dependencies._

libraryDependencies ++= Seq(
  AkkaActor         % Provided,
  AkkaHttp          % Provided,
  AkkaStream        % Provided,
  Log4jApiScala,
  AkkaHttpTestkit   % Test,
  AkkaTestkitSpecs2 % Test,
  Specs2Core        % Test)
