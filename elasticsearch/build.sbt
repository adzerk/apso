import Dependencies._

libraryDependencies ++= Seq(
  AkkaActor                  % Provided,
  ApacheHttpAsyncClient,
  ApacheHttpClient,
  ApacheHttpCore,
  CirceCore,
  Elastic4sClientEsJava,
  Elastic4sCore,
  ElasticsearchRestClient,
  AkkaActorTestkitTyped      % Test,
  AkkaHttpTestkit            % Test,
  AkkaTestkitSpecs2Classic   % Test,
  Elastic4sTestkit           % Test,
  ElasticsearchClusterRunner % Test,
  Log4JCore                  % Test,
  Log4JSlf4j                 % Test,
  Specs2Core                 % Test,
  Specs2ScalaCheck           % Test
)
