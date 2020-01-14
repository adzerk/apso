import Dependencies._

libraryDependencies ++= Seq(
  CirceCore,
  CirceGeneric,
  CirceParser,
  NscalaTime,
  SprayJson,
  Squants,
  TypesafeConfig % Provided,
  CirceLiteral   % Test,
  Specs2Core     % Test,
  Specs2JUnit    % Test)
