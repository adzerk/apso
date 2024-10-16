import ReleaseTransformations._
import spray.boilerplate.BoilerplatePlugin
import xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / organization := "com.velocidi"

ThisBuild / crossScalaVersions := Seq("2.12.20", "2.13.15")
ThisBuild / scalaVersion       := "2.13.15"

val javaVersion = "11"

// Workaround for incompatible scala-xml versions taken from https://github.com/scala/bug/issues/12632. scala-xml 1.x
// and scala-xml 2.x are "mostly" binary compatible.
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

def module(project: Project, moduleName: String) =
  (project in file(moduleName))
    .settings(name := s"apso-$moduleName")
    .settings(commonSettings: _*)

lazy val akka               = module(project, "akka")
lazy val akkaHttp           = module(project, "akka-http").dependsOn(core % Test, testkit % Test)
lazy val aws                = module(project, "aws").dependsOn(core)
lazy val caching            = module(project, "caching").enablePlugins(BoilerplatePlugin)
lazy val circe              = module(project, "circe")
lazy val collections        = module(project, "collections")
lazy val core               = module(project, "core").dependsOn(testkit % Test)
lazy val elasticsearch      = module(project, "elasticsearch").dependsOn(testkit % Test)
lazy val elasticsearchPekko = module(project, "elasticsearch-pekko").dependsOn(testkit % Test)
lazy val encryption         = module(project, "encryption")
lazy val hashing            = module(project, "hashing")
lazy val io                 = module(project, "io").dependsOn(aws, testkit % Test)
lazy val pekko              = module(project, "pekko")
lazy val pekkoHttp          = module(project, "pekko-http").dependsOn(core % Test, testkit % Test)
lazy val profiling          = module(project, "profiling")
lazy val testkit            = module(project, "testkit")
lazy val time               = module(project, "time")

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
    elasticsearchPekko,
    encryption,
    hashing,
    io,
    pekko,
    pekkoHttp,
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
    elasticsearchPekko,
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
      "VERSION" -> "0.18.8" // This version should be set to the currently released version.
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
        // Necessary for org.codelibs:elasticsearch-cluster-runner, which moved to maven.codelibs.org after v7.11.x
        "Elasticsearch Cluster Repository"  at  "https://maven.codelibs.org/",
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
      "-Xfatal-warnings",
      "-Ywarn-dead-code")

    def withCommon(flags: String*) =
      commonFlags ++ flags

    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) =>
        withCommon(
          "-Ywarn-unused-import")

      case _ =>
        withCommon(
          "-Ywarn-unused:imports")
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
      "scm:git@github.com:adzerk/apso.git"
    )
  )
  // format: on
)

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
