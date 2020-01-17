import Dependencies._

libraryDependencies ++= Seq(
  AkkaActor         % Provided,
  CirceCore,
  Log4jCore,
  ScalaLogging,
  TypesafeConfig    % Provided,
  UnirestJava,
  AkkaHttpTestkit   % Test,
  AkkaStreamTestkit % Test,
  AkkaTestkitSpecs2 % Test,
  JUnit             % Test,
  // NOTICE: This is added because of the exclusion rules on "elasticsearch-cluster-runner".
  //         While it is important to exclude those libs because of clients of this apso lib, our tests
  //         require the presence of the netty dependencies.
  Netty             % Test,
  ScalaCheck        % Test,
  Specs2Core        % Test,
  Specs2JUnit       % Test,
  Specs2ScalaCheck  % Test)
