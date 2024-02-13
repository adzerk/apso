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
  AkkaTestkit                % Test,
  AkkaHttpTestkit            % Test,
  AkkaSlf4J                  % Test,
  Elastic4sTestkit           % Test,
  ElasticsearchClusterRunner % Test,
  Log4JCore                  % Test,
  Log4JSlf4j                 % Test,
  Specs2Core                 % Test
)
