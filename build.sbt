import scalariform.formatter.preferences._
import ReleaseTransformations._
import DefaultArtifactVersions._

organization in ThisBuild := "com.velocidi"

scalaVersion in ThisBuild := "2.12.10"

def module(project: Project, moduleName: String, dependencies: sbt.librarymanagement.ModuleID*) =
  project.in(file(moduleName))
  .settings(commonSettings: _*)
  .settings(name := s"apso-$moduleName",
    libraryDependencies ++= dependencies)

lazy val core = project.in(file("core"))
  .dependsOn(testkit % "test")
  .settings(commonSettings: _*)
  .settings(
    name := "apso-core",
    libraryDependencies ++= Seq(
      "com.hierynomus"                           % "sshj"                           % "0.27.0",
      "com.mashape.unirest"                      % "unirest-java"                   % "1.4.9",
      "com.typesafe"                             % "config"                         % defaultVersion % "provided",
      "com.typesafe.akka"                       %% "akka-actor"                     % defaultVersion % "provided",
      "io.circe"                                %% "circe-core"                     % defaultVersion,
      "org.apache.logging.log4j"                %% "log4j-api-scala"                % "11.0",
      // NOTICE: This is added because of the exclusion rules on "elasticsearch-cluster-runner".
      //         While it is important to exclude those libs because of clients of this apso lib, our tests
      //         require the presence of the netty dependencies.
      "io.netty"                                 % "netty-all"                      % defaultVersion % "test",
      "com.typesafe.akka"                       %% "akka-http-testkit"              % defaultVersion % "test",
      "com.typesafe.akka"                       %% "akka-stream-testkit"            % defaultVersion % "test",
      "junit"                                    % "junit"                          % "4.12"         % "test",
      "net.ruippeixotog"                        %% "akka-testkit-specs2"            % defaultVersion % "test",
      "org.apache.logging.log4j"                 % "log4j-core"                     % "2.11.2"       % "test",
      "org.scalacheck"                          %% "scalacheck"                     % defaultVersion % "test",
      "org.specs2"                              %% "specs2-core"                    % defaultVersion % "test",
      "org.specs2"                              %% "specs2-scalacheck"              % defaultVersion % "test",
      "org.specs2"                              %% "specs2-junit"                   % defaultVersion % "test"))

lazy val testkit = project.in(file("testkit"))
  .settings(commonSettings: _*)
  .settings(
    name := "apso-testkit",
    libraryDependencies ++= Seq(
      "com.sksamuel.elastic4s"                  %% "elastic4s-core"                % defaultVersion % "provided",
      "com.sksamuel.elastic4s"                  %% "elastic4s-client-esjava"       % defaultVersion % "provided",
      // FIXME: netty-all conflicts with all non-bundle netty dependencies, which are needed by GRPC and possibly others.
      "org.codelibs"                             % "elasticsearch-cluster-runner"  % "7.1.1.0" excludeAll ExclusionRule(organization = "io.netty"),
      "com.sksamuel.elastic4s"                  %% "elastic4s-testkit"             % defaultVersion,
      "com.typesafe.akka"                       %% "akka-testkit"                  % defaultVersion % "provided",
      "com.typesafe.akka"                       %% "akka-http-testkit"             % defaultVersion % "provided",
      "com.typesafe.akka"                       %% "akka-stream-testkit"           % defaultVersion % "provided",
      "org.specs2"                              %% "specs2-core"                   % defaultVersion % "provided"))

lazy val json = module(project, "json",
      "com.github.nscala-time"                  %% "nscala-time"                   % defaultVersion,
      "com.typesafe"                             % "config"                        % defaultVersion % "provided",
      "io.circe"                                %% "circe-core"                    % defaultVersion,
      "io.circe"                                %% "circe-generic"                 % defaultVersion,
      "io.circe"                                %% "circe-parser"                  % defaultVersion,
      "io.spray"                                %% "spray-json"                    % "1.3.5",
      "org.typelevel"                           %% "squants"                       % "1.5.0",
      "io.circe"                                %% "circe-literal"                 % defaultVersion % "test",
      "org.specs2"                              %% "specs2-core"                   % defaultVersion % "test",
      "org.specs2"                              %% "specs2-junit"                  % defaultVersion % "test")
  .dependsOn(core)
  .dependsOn(collections)

lazy val aws = module(project, "aws",
      "com.amazonaws"                            % "aws-java-sdk-s3"               % defaultVersion % "provided",
      "com.typesafe"                             % "config"                        % defaultVersion % "provided",
      "org.specs2"                              %% "specs2-core"                   % defaultVersion % "test")
  .dependsOn(core)

lazy val io = module(project, "io",
      "com.amazonaws"                            % "aws-java-sdk-s3"               % defaultVersion % "provided",
      "com.typesafe"                             % "config"                        % defaultVersion % "provided",
      "io.github.andrebeat"                     %% "scala-pool"                    % "0.4.1",
      "org.specs2"                              %% "specs2-core"                   % defaultVersion % "test")
  .dependsOn(aws)
  .dependsOn(testkit % "test")

lazy val collections = module(project, "collections",
      "org.scalacheck"                          %% "scalacheck"                    % defaultVersion % "test",
      "org.specs2"                              %% "specs2-core"                   % defaultVersion % "test",
      "org.specs2"                              %% "specs2-scalacheck"             % defaultVersion % "test")

lazy val elasticsearch = module(project, "elasticsearch",
      "com.sksamuel.elastic4s"                  %% "elastic4s-core"                % defaultVersion % "provided",
      "com.sksamuel.elastic4s"                  %% "elastic4s-client-esjava"       % defaultVersion % "provided",
      "com.typesafe.akka"                       %% "akka-actor"                    % defaultVersion % "provided",
      // NOTICE: This is added because of the exclusion rules on "elasticsearch-cluster-runner".
      //         While it is important to exclude those libs because of clients of this apso lib, our tests
      //         require the presence of the netty dependencies.
      "io.netty"                                 % "netty-all"                     % defaultVersion % "test",
      "com.typesafe.akka"                       %% "akka-http-testkit"             % defaultVersion % "test",
      "net.ruippeixotog"                        %% "akka-testkit-specs2"           % defaultVersion % "test",
      "org.specs2"                              %% "specs2-core"                   % defaultVersion % "test",
      "org.specs2"                              %% "specs2-scalacheck"             % defaultVersion % "test")
  .dependsOn(core)
  .dependsOn(testkit % "test")

lazy val time = module(project, "time",
      "com.github.nscala-time"                  %% "nscala-time"                   % defaultVersion,
      "org.specs2"                              %% "specs2-core"                   % defaultVersion % "test")

lazy val caching = module(project, "caching",
      "com.googlecode.concurrentlinkedhashmap"   % "concurrentlinkedhashmap-lru"   % "1.4.2",
      "net.ruippeixotog"                        %% "akka-testkit-specs2"           % defaultVersion % "test",
      "org.specs2"                              %% "specs2-core"                   % defaultVersion % "test")

lazy val encryption = module(project, "encryption",
      "commons-codec"                            % "commons-codec"                 % "1.12")
  .dependsOn(core)

lazy val hashing = module(project, "hashing",
      "com.joyent.util"                          % "fast-md5"                      % "2.7.1")

lazy val profiling = module(project, "profiling",
      "com.j256.simplejmx"                       % "simplejmx"                     % "1.17",
      "org.apache.logging.log4j"                 % "log4j-api"                     % "2.11.2",
      "org.apache.logging.log4j"                %% "log4j-api-scala"               % "11.0")
  .dependsOn(core)

lazy val spray = module(project, "spray",
      "com.typesafe.akka"                       %% "akka-actor"                    % defaultVersion % "provided",
      "com.typesafe.akka"                       %% "akka-http"                     % defaultVersion % "provided",
      "com.typesafe.akka"                       %% "akka-stream"                   % defaultVersion % "provided",
      "com.typesafe.akka"                       %% "akka-http-testkit"             % defaultVersion % "test",
      "net.ruippeixotog"                        %% "akka-testkit-specs2"           % defaultVersion % "test",
      "org.specs2"                              %% "specs2-core"                   % defaultVersion % "test")
  .dependsOn(core)
  .dependsOn(testkit % "test")

lazy val apso = (project in file("."))
  .settings(commonSettings: _*)
  .settings(name := "apso")
  .dependsOn(core, json, aws, io, collections, elasticsearch, time, caching, encryption, hashing, profiling, spray)
  .aggregate(core, json, aws, io, collections, elasticsearch, time, caching, encryption, hashing, profiling, spray, testkit)

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.typesafeRepo("snapshots"),
    "Spray Repository"              at "http://repo.spray.io/",
    "Bintray Scalaz Releases"       at "http://dl.bintray.com/scalaz/releases",
    "JCenter Repository"            at "http://jcenter.bintray.com/"),

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
  releaseStepCommandAndRemaining("test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("publishSigned"),
  setNextVersion,
  commitNextVersion,
  pushChanges,
  releaseStepCommandAndRemaining("sonatypeReleaseAll"))
