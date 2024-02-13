import Dependencies._

libraryDependencies ++= Seq(
  PekkoActor                  % Provided,
  ApacheHttpAsyncClient,
  ApacheHttpClient,
  ApacheHttpCore,
  CirceCore,
  Elastic4sClientEsJava,
  Elastic4sCore,
  ElasticsearchRestClient,
  PekkoTestkit                % Test,
  PekkoHttpTestkit            % Test,
  PekkoSlf4J                  % Test,
  Elastic4sTestkit            % Test,
  ElasticsearchClusterRunner  % Test,
  Log4JCore                   % Test,
  Log4JSlf4j                  % Test,
  Specs2Core                  % Test
)
