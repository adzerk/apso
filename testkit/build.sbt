import Dependencies._

libraryDependencies ++= Seq(
  Elastic4sClientEsJava,
  Elastic4sCore,
  Elastic4sTestkit,
  Elasticsearch,
  ElasticsearchClusterRunner,
  ScalaTestCore,
  Specs2Common  % Provided,
  Specs2Core    % Provided,
  Specs2Matcher % Provided
)
