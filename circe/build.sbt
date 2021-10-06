import Dependencies._

libraryDependencies ++= Seq(
  CatsCore,
  CirceCore,
  CirceGeneric,
  CirceParser,
  JodaTime       % Provided,
  ScalaCollectionCompat,
  Shapeless,
  Squants        % Provided,
  TypesafeConfig % Provided,
  CirceLiteral   % Test,
  Specs2Core     % Test,
  Specs2JUnit    % Test
)
