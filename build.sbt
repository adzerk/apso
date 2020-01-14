import scalariform.formatter.preferences._
import ReleaseTransformations._
import Dependencies._

organization in ThisBuild := "com.velocidi"

scalaVersion in ThisBuild := "2.12.10"

def module(project: Project, moduleName: String, dependencies: sbt.librarymanagement.ModuleID*) =
  (project in file(moduleName))
    .settings(commonSettings: _*)
    .settings(
      name := s"apso-$moduleName",
      libraryDependencies ++= dependencies)

lazy val core = module(project, "core",
  UnirestJava,
  TypesafeConfig % Provided,
  AkkaActor % Provided,
  CirceCore,
  Log4jApiScala,
  Log4jCore,
  // NOTICE: This is added because of the exclusion rules on "elasticsearch-cluster-runner".
  //         While it is important to exclude those libs because of clients of this apso lib, our tests
  //         require the presence of the netty dependencies.
  Netty % Test,
  AkkaHttpTestkit % Test,
  AkkaStreamTestkit % Test,
  JUnit % Test,
  AkkaTestkitSpecs2 % Test,
  ScalaCheck % Test,
  Specs2Core % Test,
  Specs2ScalaCheck % Test,
  Specs2JUnit % Test)
  .dependsOn(testkit % Test)

lazy val testkit = module(project, "testkit",
  Elastic4sCore % Provided,
  Elastic4sClientEsJava % Provided,
  // FIXME: netty-all conflicts with all non-bundle netty dependencies, which are needed by GRPC and possibly others.
  ElasticsearchClusterRunner,
  Elastic4sTestkit,
  AkkaTestkit % Provided,
  AkkaHttpTestkit % Provided,
  AkkaStreamTestkit % Provided,
  Specs2Core % Provided)

lazy val json = module(project, "json",
  NscalaTime,
  TypesafeConfig % Provided,
  CirceCore,
  CirceGeneric,
  CirceParser,
  SprayJson,
  Squants,
  CirceLiteral % Test,
  Specs2Core % Test,
  Specs2JUnit % Test)
  .dependsOn(core)
  .dependsOn(collections)

lazy val aws = module(project, "aws",
  AwsJavaSdkS3 % Provided,
  TypesafeConfig % Provided,
  Log4jApiScala,
  Specs2Core % Test)
  .dependsOn(core)

lazy val io = module(project, "io",
  AwsJavaSdkS3 % Provided,
  SshJ,
  TypesafeConfig % Provided,
  ScalaPool,
  Log4jApiScala,
  Specs2Core % Test)
  .dependsOn(aws)
  .dependsOn(testkit % Test)

lazy val collections = module(project, "collections",
  ScalaCheck % Test,
  Specs2Core % Test,
  Specs2ScalaCheck % Test)

lazy val elasticsearch = module(project, "elasticsearch",
  Elastic4sCore % Provided,
  Elastic4sClientEsJava % Provided,
  AkkaActor % Provided,
  AkkaStream % Provided,
  CirceCore,
  Log4jApiScala,
  // NOTICE: This is added because of the exclusion rules on "elasticsearch-cluster-runner".
  //         While it is important to exclude those libs because of clients of this apso lib, our tests
  //         require the presence of the netty dependencies.
  Netty % Test,
  AkkaHttpTestkit % Test,
  AkkaTestkitSpecs2 % Test,
  Specs2Core % Test,
  Specs2ScalaCheck % Test)
  .dependsOn(core)
  .dependsOn(testkit % Test)

lazy val time = module(project, "time",
  NscalaTime,
  Specs2Core % Test)

lazy val caching = module(project, "caching",
  ConcurrentLinkedHashMapLru,
  AkkaTestkitSpecs2 % Test,
  Specs2Core % Test)

lazy val encryption = module(project, "encryption",
  BouncyCastleProvider,
  BouncyCastlePkix,
  Log4jApiScala,
  CommonsCodec)
  .dependsOn(core)

lazy val hashing = module(project, "hashing",
  FastMd5)

lazy val profiling = module(project, "profiling",
  SimpleJmx,
  Log4jApiScala)
  .dependsOn(core)

lazy val akkaHttp = module(project, "akka-http",
  AkkaActor % Provided,
  AkkaHttp % Provided,
  AkkaStream % Provided,
  Log4jApiScala,
  AkkaHttpTestkit % Test,
  AkkaTestkitSpecs2 % Test,
  Specs2Core % Test)
  .dependsOn(core)
  .dependsOn(testkit % Test)

lazy val apso = (project in file("."))
  .settings(commonSettings: _*)
  .settings(name := "apso")
  .dependsOn(core, json, aws, io, collections, elasticsearch, time, caching, encryption, hashing, profiling, akkaHttp)
  .aggregate(core, json, aws, io, collections, elasticsearch, time, caching, encryption, hashing, profiling, akkaHttp, testkit)

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
