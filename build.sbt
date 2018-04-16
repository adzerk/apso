import scalariform.formatter.preferences._
import ReleaseTransformations._

organization in ThisBuild := "eu.shiftforward"

scalaVersion in ThisBuild := "2.12.4"
crossScalaVersions in ThisBuild := Seq("2.11.11", "2.12.4")

lazy val core = project.in(file("core"))
  .dependsOn(testkit % "test")
  .settings(commonSettings: _*)
  .settings(
    name := "apso",
    libraryDependencies ++= Seq(
      "com.amazonaws"                              % "aws-java-sdk-ec2"               % "1.11.221"       % "provided", // updating this to patch 290 or 292
      "com.amazonaws"                              % "aws-java-sdk-s3"                % "1.11.221"       % "provided", // makes the travis build hang for more than 10min
      "com.chuusai"                               %% "shapeless"                      % "2.3.3",
      "com.github.nscala-time"                    %% "nscala-time"                    % "2.18.0",
      "com.googlecode.concurrentlinkedhashmap"     % "concurrentlinkedhashmap-lru"    % "1.4.2",
      "com.hierynomus"                             % "sshj"                           % "0.23.0",
      "com.j256.simplejmx"                         % "simplejmx"                      % "1.15",
      "com.jcraft"                                 % "jzlib"                          % "1.1.3",
      "com.mashape.unirest"                        % "unirest-java"                   % "1.4.9",
      "com.twmacinta"                              % "fast-md5"                       % "2.7.1",
      "com.typesafe"                               % "config"                         % "1.3.3"          % "provided",
      "com.typesafe.akka"                         %% "akka-actor"                     % "2.5.11"         % "provided",
      "com.typesafe.akka"                         %% "akka-http"                      % "10.1.0"         % "provided",
      "com.typesafe.akka"                         %% "akka-stream"                    % "2.5.11"         % "provided",
      "commons-codec"                              % "commons-codec"                  % "1.11",
      "io.circe"                                  %% "circe-core"                     % "0.9.1",
      "io.circe"                                  %% "circe-parser"                   % "0.9.1",
      "io.github.andrebeat"                       %% "scala-pool"                     % "0.4.1",
      "io.spray"                                  %% "spray-json"                     % "1.3.4"          % "provided",
      "org.apache.logging.log4j"                   % "log4j-api"                      % "2.10.0",
      "org.apache.logging.log4j"                  %% "log4j-api-scala"                % "11.0",
      "org.bouncycastle"                           % "bcpkix-jdk15on"                 % "1.59",
      "org.bouncycastle"                           % "bcprov-jdk15on"                 % "1.59",
      "org.scalaz"                                %% "scalaz-core"                    % "7.2.20"         % "provided",
      "org.apache.logging.log4j"                   % "log4j-core"                     % "2.10.0"         % "test",
      "com.typesafe.akka"                         %% "akka-http-testkit"              % "10.1.0"         % "test",
      "junit"                                      % "junit"                          % "4.12"           % "test",
      "net.ruippeixotog"                          %% "akka-testkit-specs2"            % "0.2.3"          % "test",
      "org.scalacheck"                            %% "scalacheck"                     % "1.13.5"         % "test",
      "org.specs2"                                %% "specs2-core"                    % "4.0.3"          % "test",
      "org.specs2"                                %% "specs2-scalacheck"              % "4.0.3"          % "test",
      "org.specs2"                                %% "specs2-junit"                   % "4.0.3"          % "test"))

lazy val testkit = project.in(file("testkit"))
  .settings(commonSettings: _*)
  .settings(
    name := "apso-testkit",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"             %% "akka-testkit"       % "2.5.11"          % "provided",
      "com.typesafe.akka"             %% "akka-http-testkit"  % "10.1.0"          % "provided",
      "org.apache.logging.log4j"       % "log4j-api"          % "2.10.0",
      "org.apache.logging.log4j"      %% "log4j-api-scala"    % "11.0",
      "org.specs2"                    %% "specs2-core"        % "4.0.3"           % "provided",
      "org.specs2"                    %% "specs2-junit"       % "4.0.3"           % "provided"))

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.typesafeRepo("snapshots"),
    "Spray Repository"              at "http://repo.spray.io/",
    "Bintray Scalaz Releases"       at "http://dl.bintray.com/scalaz/releases",
    "JCenter Repository"            at "http://jcenter.bintray.com/",
    "JAnalyse Repository"           at "http://www.janalyse.fr/repository/"),

  scalariformPreferences := scalariformPreferences.value
    .setPreference(DanglingCloseParenthesis, Prevent)
    .setPreference(DoubleIndentConstructorArguments, true),

  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Ywarn-unused-import",
    "-Xlint:-adapted-args,-nullary-override,_"),

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },

  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },

  licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/velocidi/apso")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/velocidi/apso"),
      "scm:git@github.com:velocidi/apso.git"
    )
  ))

// do not publish the root project
skip in publish := true

releaseTagComment := s"Release ${(version in ThisBuild).value}"
releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}"

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("+test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  setNextVersion,
  commitNextVersion,
  pushChanges,
  releaseStepCommandAndRemaining("sonatypeReleaseAll"))
