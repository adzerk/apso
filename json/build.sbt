import Dependencies._

libraryDependencies ++= Seq(
  CatsCore,
  CirceCore,
  CirceGeneric,
  CirceParser,
  JodaTime       % Provided,
  Shapeless,
  Squants        % Provided,
  TypesafeConfig % Provided,
  CirceLiteral   % Test,
  Specs2Core     % Test,
  Specs2JUnit    % Test
)
