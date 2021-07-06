import ReleaseTransformations._
import spray.boilerplate.BoilerplatePlugin

ThisBuild / organization := "com.velocidi"

ThisBuild / crossScalaVersions := Seq("2.12.14", "2.13.6")
ThisBuild / scalaVersion := "2.12.14"

def module(project: Project, moduleName: String) =
  (project in file(moduleName))
    .settings(name := s"apso-$moduleName")
    .settings(commonSettings: _*)

lazy val akka          = module(project, "akka")
lazy val akkaHttp      = module(project, "akka-http").dependsOn(log, core % Test, testkit % Test)
lazy val aws           = module(project, "aws").dependsOn(core, log)
lazy val caching       = module(project, "caching").enablePlugins(BoilerplatePlugin)
lazy val circe         = module(project, "circe")
lazy val collections   = module(project, "collections")
lazy val core          = module(project, "core").dependsOn(testkit % Test)
lazy val elasticsearch = module(project, "elasticsearch").dependsOn(log, testkit % Test)
lazy val encryption    = module(project, "encryption").dependsOn(log)
lazy val hashing       = module(project, "hashing")
lazy val io            = module(project, "io").dependsOn(aws, testkit % Test)
lazy val log           = module(project, "log")
lazy val profiling     = module(project, "profiling").dependsOn(core, log)
lazy val testkit       = module(project, "testkit")
lazy val time          = module(project, "time")

lazy val apso = (project in file("."))
  .settings(commonSettings: _*)
  .settings(name := "apso")
  .dependsOn(
    akka,
    akkaHttp,
    aws,
    caching,
    circe,
    collections,
    core,
    elasticsearch,
    encryption,
    hashing,
    io,
    log,
    profiling,
    time
  )
  .aggregate(
    akka,
    akkaHttp,
    aws,
    caching,
    circe,
    collections,
    core,
    elasticsearch,
    encryption,
    hashing,
    io,
    log,
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
      "VERSION" -> "0.16.10" // This version should be set to the currently released version.
    ),

    publish / skip := true
    // format: on
  )
  .enablePlugins(MdocPlugin)

lazy val commonSettings = Seq(
  // format: off
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.typesafeRepo("snapshots"),
    "Spray Repository"              at "https://repo.spray.io/",
    "Bintray Scalaz Releases"       at "https://dl.bintray.com/scalaz/releases",
    "JCenter Repository"            at "https://jcenter.bintray.com/"),

  scalafmtOnCompile := true,

  // Enable Scalafix and the OrganizeImports rule.
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
      "-Ywarn-dead-code")

    def withCommon(flags: String*) =
      commonFlags ++ flags

    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) =>
        withCommon(
          "-deprecation",
          "-Xfatal-warnings",
          "-Ywarn-unused-import")

      case _ =>
        withCommon(
          "-Ywarn-unused:imports")
    }
  },

  autoAPIMappings := true,

  publishTo := sonatypePublishToBundle.value,

  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },

  licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/velocidi/apso")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/velocidi/apso"),
      "scm:git@github.com:velocidi/apso.git"
    )
  )
  // format: on
)

// Enable the OrganizeImports Scalafix rule.
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

releaseCrossBuild := true
releaseTagComment := s"Release ${(ThisBuild / version).value}"
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
