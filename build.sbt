import Dependencies._
import ReleaseTransformations._
import spray.boilerplate.BoilerplatePlugin
import xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / organization := "com.kevel"

// Setting no cross build for the aggregating root project so that we can have proper per project exclusions.
ThisBuild / crossScalaVersions := Nil
ThisBuild / scalaVersion       := Versions.Scala213

val javaVersion = "11"

// Workaround for incompatible scala-xml versions taken from https://github.com/scala/bug/issues/12632. scala-xml 1.x
// and scala-xml 2.x are "mostly" binary compatible.
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

def module(project: Project, moduleName: String) =
  (project in file(moduleName))
    .settings(name := s"apso-$moduleName")
    .settings(commonSettings: _*)

lazy val aws = module(project, "aws")
  .dependsOn(core)
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(
      ScalaLogging,
      AwsJavaSdkS3,
      AwsJavaSdkCore,
      ScalaLogging,
      TypesafeConfig,
      Specs2Core % Test
    )
  )

lazy val caching = module(project, "caching")
  .enablePlugins(BoilerplatePlugin)
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(
      Scaffeine,
      Specs2Core % Test
    )
  )

lazy val circe = module(project, "circe")
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(
      CatsCore,
      CirceCore,
      CirceGeneric,
      CirceParser,
      JodaTime       % Provided,
      Squants        % Provided,
      TypesafeConfig % Provided,
      CirceLiteral   % Test,
      Specs2Core     % Test,
      Specs2JUnit    % Test
    )
  )

lazy val collections = module(project, "collections")
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(
      ScalaCheck       % Test,
      Specs2Core       % Test,
      Specs2ScalaCheck % Test
    )
  )

lazy val core = module(project, "core")
  .dependsOn(testkit % Test)
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(
      CirceCore,
      ScalaLogging,
      TypesafeConfig   % Provided,
      UnirestJava,
      JUnit            % Test,
      ScalaCheck       % Test,
      Specs2Core       % Test,
      Specs2JUnit      % Test,
      Specs2ScalaCheck % Test
    )
  )

lazy val encryption = module(project, "encryption")
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(BouncyCastlePkix % Runtime, BouncyCastleProvider, CommonsCodec, ScalaLogging)
  )

lazy val hashing = module(project, "hashing")
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(FastMd5)
  )

lazy val io = module(project, "io")
  .dependsOn(aws, testkit % Test)
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(
      AwsJavaSdkCore,
      AwsJavaSdkS3,
      // FIX: Explicitly override transient bouncy castle versions from `sshj`: https://security.snyk.io/vuln/SNYK-JAVA-ORGBOUNCYCASTLE-6612984
      // Remove once com.hierynomus:sshj releases a version with https://github.com/hierynomus/sshj/pull/938
      BouncyCastlePkix,
      BouncyCastleProvider,
      ScalaLogging,
      // FIXME: scala-pool is not avaiable for Scala 3, but we can use the Scala 2.13 version. This is currently only
      //        being used to manage pools of SFTP clients for the same connection details. We should eventually
      //        consider an alternative implementation.
      ScalaPool.cross(CrossVersion.for3Use2_13),
      SshJ,
      TypesafeConfig,
      Specs2Core % Test
    )
  )

lazy val pekko = module(project, "pekko")
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(PekkoActor % Provided)
  )

lazy val pekkoHttp = module(project, "pekko-http")
  .dependsOn(core % Test, testkit % Test)
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(
      ScalaLogging,
      PekkoActor             % Provided,
      PekkoHttp              % Provided,
      PekkoHttpCore          % Provided,
      PekkoStream            % Provided,
      ScalaLogging,
      TypesafeConfig,
      PekkoActorTestkitTyped % Test,
      PekkoHttpTestkit       % Test,
      Specs2Core             % Test
    )
  )

lazy val profiling = module(project, "profiling")
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(ScalaLogging, SimpleJmx)
  )

lazy val testkit = module(project, "testkit")
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(
      ScalaTestCore,
      Specs2Common  % Provided,
      Specs2Core    % Provided,
      Specs2Matcher % Provided
    )
  )

lazy val time = module(project, "time")
  .settings(
    crossScalaVersions := List(Versions.Scala213, Versions.Scala3),
    libraryDependencies ++= Seq(JodaTime, NscalaTime, Specs2Core % Test)
  )

lazy val apso = (project in file("."))
  .settings(commonSettings: _*)
  .settings(name := "apso", crossScalaVersions := List(Versions.Scala213))
  .dependsOn(
    aws,
    caching,
    circe,
    collections,
    core,
    encryption,
    hashing,
    io,
    pekko,
    pekkoHttp,
    profiling,
    time
  )
  .aggregate(
    aws,
    caching,
    circe,
    collections,
    core,
    encryption,
    hashing,
    io,
    pekko,
    pekkoHttp,
    profiling,
    testkit,
    time
  )

lazy val docs = (project in file("apso-docs"))
  .dependsOn(apso)
  .settings(commonSettings: _*)
  .settings(
    // format: off
    mdocOut := (ThisBuild / baseDirectory).value,

    mdocVariables := Map(
      "VERSION" -> "0.21.1" // This version should be set to the currently released version.
    ),

    publish / skip := true
    // format: on
  )
  .enablePlugins(MdocPlugin)

lazy val commonSettings = Seq(
  // format: off
  resolvers ++=
    Resolver.sonatypeOssRepos("snapshots") ++
      Seq(
        Resolver.typesafeRepo("snapshots"),
        "Spray Repository"                  at  "https://repo.spray.io/",
        "Bintray Scalaz Releases"           at  "https://dl.bintray.com/scalaz/releases",
        "JCenter Repository"                at  "https://jcenter.bintray.com/"
      ),

  scalafmtOnCompile := true,

  // Enable Scalafix.
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  scalafixOnCompile := true,

  scalacOptions ++= {
    lazy val commonFlags = Seq(
      "-encoding", "UTF-8",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-release", javaVersion,
      "-Xfatal-warnings")

    def withCommon(flags: String*) =
      commonFlags ++ flags

    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        withCommon("-Ywarn-dead-code", "-Ywarn-unused:imports")

      case Some((3, _)) =>
        withCommon("-Wunused:imports")

      case _ =>
        throw new RuntimeException("Unsupported Scala version")
    }
  },

  javacOptions ++= List(
    "--release", javaVersion
  ),

  autoAPIMappings := true,

  publishTo := sonatypePublishToBundle.value,

  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },

  licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/adzerk/apso")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/adzerk/apso"),
      "scm:git:ssh://git@github.com/adzerk/apso.git"
    )
  )
  // format: on
)

ThisBuild / sonatypeCredentialHost := sonatypeCentralHost

releaseCrossBuild    := true
releaseTagComment    := s"Release ${(ThisBuild / version).value}"
releaseCommitMessage := s"Set version to ${(ThisBuild / version).value}"

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
