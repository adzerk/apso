import Dependencies._

libraryDependencies ++= Seq(
  ScalaLogging,
  AkkaActor                  % Provided,
  ApacheHttpAsyncClient,
  ApacheHttpClient,
  ApacheHttpCore,
  CirceCore,
  Elastic4sClientEsJava,
  Elastic4sCore,
  ElasticsearchRestClient,
  ScalaLogging,
  AkkaActorTestkitTyped      % Test,
  AkkaHttpTestkit            % Test,
  AkkaTestkitSpecs2Classic   % Test,
  Elastic4sTestkit           % Test,
  ElasticsearchClusterRunner % Test,
  Specs2Core                 % Test,
  Specs2ScalaCheck           % Test
)
