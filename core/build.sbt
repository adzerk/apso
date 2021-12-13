import Dependencies._

libraryDependencies ++= Seq(
  ApacheHttpAsyncClient,
  ApacheHttpClient,
  ApacheHttpCore,
  CirceCore,
  ScalaCollectionCompat,
  ScalaLogging,
  TypesafeConfig    % Provided,
  UnirestJava,
  AkkaHttpTestkit   % Test,
  AkkaStreamTestkit % Test,
  AkkaTestkitSpecs2 % Test,
  JUnit             % Test,
  ScalaCheck        % Test,
  Specs2Core        % Test,
  Specs2JUnit       % Test,
  Specs2ScalaCheck  % Test
)
