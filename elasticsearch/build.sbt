import Dependencies._

libraryDependencies ++= Seq(
  AkkaActor             % Provided,
  AkkaStream            % Provided,
  CirceCore,
  Elastic4sClientEsJava % Provided,
  Elastic4sCore         % Provided,
  ScalaLogging,
  AkkaHttpTestkit       % Test,
  AkkaTestkitSpecs2     % Test,
  // NOTICE: This is added because of the exclusion rules on "elasticsearch-cluster-runner".
  //         While it is important to exclude those libs because of clients of this apso lib, our tests
  //         require the presence of the netty dependencies.
  Netty                 % Test,
  Specs2Core            % Test,
  Specs2ScalaCheck      % Test)
