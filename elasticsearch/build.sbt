import Dependencies._

libraryDependencies ++= Seq(
  AkkaActor             % Provided,
  AkkaActorTyped        % Provided,
  AkkaStream            % Provided,
  CirceCore,
  Elastic4sClientEsJava % Provided,
  Elastic4sCore         % Provided,
  ScalaLogging,
  AkkaActorTestkitTyped % Test,
  AkkaHttpTestkit       % Test,
  AkkaTestkitSpecs2     % Test,
  Specs2Core            % Test,
  Specs2ScalaCheck      % Test)
