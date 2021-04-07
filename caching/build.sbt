import Dependencies._

libraryDependencies ++= Seq(
  "com.github.cb372" %% "scalacache-caffeine" % "0.28.0",
  "com.github.cb372" %% "scalacache-guava"    % "0.28.0",
  ConcurrentLinkedHashMapLru,
  AkkaTestkitSpecs2 % Test,
  Specs2Core % Test)

