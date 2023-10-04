import Dependencies._

libraryDependencies ++= Seq(
  ScalaLogging,
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
