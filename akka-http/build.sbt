import Dependencies._

libraryDependencies ++= Seq(
  AkkaActor,
  AkkaHttp                % Provided,
  AkkaHttpCore            % Provided,
  AkkaStream,
  ScalaLogging,
  TypesafeConfig,
  AkkaActorTestkitTyped   % Test,
  AkkaHttpTestkit         % Test,
  AkkaTestkitSpecs2       % Test,
  Specs2Core              % Test)
