import Dependencies._

libraryDependencies ++= Seq(
  CirceCore,
  ScalaCollectionCompat,
  ScalaLogging,
  TypesafeConfig           % Provided,
  UnirestJava,
  AkkaHttpTestkit          % Test,
  AkkaStreamTestkit        % Test,
  AkkaTestkitSpecs2Classic % Test,
  JUnit                    % Test,
  ScalaCheck               % Test,
  Specs2Core               % Test,
  Specs2JUnit              % Test,
  Specs2ScalaCheck         % Test
)
