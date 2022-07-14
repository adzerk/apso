import Dependencies._

libraryDependencies ++= Seq(
  AkkaActor                % Provided,
  AkkaHttp                 % Provided,
  AkkaHttpCore             % Provided,
  AkkaStream               % Provided,
  ScalaLogging,
  TypesafeConfig,
  AkkaActorTestkitTyped    % Test,
  AkkaHttpTestkit          % Test,
  AkkaTestkitSpecs2Classic % Test,
  Specs2Core               % Test
)
