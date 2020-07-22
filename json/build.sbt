import Dependencies._

libraryDependencies ++= Seq(
  CirceCore,
  CirceGeneric,
  CirceParser,
  NscalaTime,
  Squants,
  TypesafeConfig % Provided,
  CirceLiteral   % Test,
  Specs2Core     % Test,
  Specs2JUnit    % Test)
