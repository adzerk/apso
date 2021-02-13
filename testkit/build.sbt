import Dependencies._

libraryDependencies ++= Seq(
  AkkaHttpTestkit       % Provided,
  AkkaStreamTestkit     % Provided,
  AkkaTestkit           % Provided,
  Elastic4sClientEsJava % Provided,
  Elastic4sCore         % Provided,
  Elastic4sTestkit,
  ElasticsearchClusterRunner,
  Specs2Core            % Provided
)
