import Dependencies._

libraryDependencies ++= Seq(
  AkkaActor,
  AkkaHttp              % Provided,
  AkkaStream,
  ScalaLogging,
  AkkaActorTestkitTyped % Test,
  AkkaHttpTestkit       % Test,
  AkkaTestkitSpecs2     % Test,
  Specs2Core            % Test
)
