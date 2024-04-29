import Dependencies._

libraryDependencies ++= Seq(
  PekkoActor                   % Provided,
  ApacheHttpAsyncClient,
  ApacheHttpClient,
  ApacheHttpCore,
  CirceCore,
  Elastic4sClientEsJava,
  Elastic4sCore,
  ElasticsearchRestClient,
  // This is explicitly included to force the eviction of a dependency exposing the following direct vulnerabilities:
  // CVE-2022-42004, CVE-2022-42003 and CVE-2020-36518.
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.5",
  PekkoTestkit                 % Test,
  PekkoHttpTestkit             % Test,
  PekkoSlf4J                   % Test,
  Elastic4sTestkit             % Test,
  ElasticsearchClusterRunner   % Test,
  Log4JCore                    % Test,
  Log4JSlf4j                   % Test,
  Specs2Core                   % Test
)
