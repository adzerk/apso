import Dependencies._

libraryDependencies ++= Seq(
  "com.github.ben-manes.caffeine" % "caffeine"            % "2.7.0",    // This wasn't updated due to incompatibility with scalacache-caffeine
  "com.github.cb372"             %% "scalacache-caffeine" % "0.28.0",
  "com.github.cb372"             %% "scalacache-core"     % "0.28.0",
  "com.github.cb372"             %% "scalacache-guava"    % "0.28.0",
  "com.google.guava"              % "guava"               % "28.0-jre", // This wasn't updated due to incompatibility with scalacache-guava
  ConcurrentLinkedHashMapLru,
  ScalaCollectionCompat,
  Specs2Core                      % Test
)
