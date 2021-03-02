import Dependencies._

libraryDependencies ++= Seq(
  AkkaActor             % Provided,
  ApacheHttpAsyncClient,
  ApacheHttpClient,
  ApacheHttpCore,
  CirceCore,
  Elastic4sClientEsJava,
  Elastic4sCore,
  ElasticsearchRestClient,
  ScalaLogging,
  AkkaActorTestkitTyped % Test,
  AkkaHttpTestkit       % Test,
  AkkaTestkitSpecs2     % Test,
  Specs2Core            % Test,
  Specs2ScalaCheck      % Test)
