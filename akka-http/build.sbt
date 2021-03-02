import Dependencies._

libraryDependencies ++= Seq(
  AkkaActor             % Provided,
  AkkaHttp              % Provided,
  AkkaStream            % Provided,
  ScalaLogging,
  AkkaActorTestkitTyped % Test,
  AkkaHttpTestkit       % Test,
  AkkaTestkitSpecs2     % Test,
  Specs2Core            % Test
)
