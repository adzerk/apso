import Dependencies._

libraryDependencies ++= Seq(
  AkkaActor             % Provided,
  AkkaStream            % Provided,
  CirceCore,
  Elastic4sClientEsJava % Provided,
  Elastic4sCore         % Provided,
  ScalaLogging,
  AkkaHttpTestkit       % Test,
  AkkaTestkitSpecs2     % Test,
  Specs2Core            % Test,
  Specs2ScalaCheck      % Test)
