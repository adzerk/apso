import Dependencies._

libraryDependencies ++= Seq(
  ScalaLogging,
  PekkoActor                % Provided,
  PekkoHttp                 % Provided,
  PekkoHttpCore             % Provided,
  PekkoStream               % Provided,
  ScalaLogging,
  TypesafeConfig,
  PekkoActorTestkitTyped    % Test,
  PekkoHttpTestkit          % Test,
  Specs2Core                % Test
)
