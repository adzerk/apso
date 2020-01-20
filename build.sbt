import scalariform.formatter.preferences._
import ReleaseTransformations._

organization in ThisBuild := "com.velocidi"

crossScalaVersions in ThisBuild := Seq("2.12.10", "2.13.1")
scalaVersion in ThisBuild := "2.12.10"

def module(project: Project, moduleName: String) =
  (project in file(moduleName))
    .settings(name := s"apso-$moduleName")
    .settings(commonSettings: _*)

lazy val akkaHttp      = module(project, "akka-http").dependsOn(core, testkit % Test)
lazy val aws           = module(project, "aws").dependsOn(core)
lazy val caching       = module(project, "caching")
lazy val collections   = module(project, "collections")
lazy val core          = module(project, "core").dependsOn(testkit % Test)
lazy val elasticsearch = module(project, "elasticsearch").dependsOn(core, testkit % Test)
lazy val encryption    = module(project, "encryption").dependsOn(core)
lazy val hashing       = module(project, "hashing")
lazy val io            = module(project, "io").dependsOn(aws, testkit % Test)
lazy val json          = module(project, "json").dependsOn(core, collections)
lazy val profiling     = module(project, "profiling").dependsOn(core)
lazy val testkit       = module(project, "testkit")
lazy val time          = module(project, "time")

lazy val apso = (project in file("."))
  .settings(commonSettings: _*)
  .settings(name := "apso")
  .dependsOn(akkaHttp, aws, caching, collections, core, elasticsearch, encryption, hashing, io, json, profiling, time)
  .aggregate(akkaHttp, aws, caching, collections, core, elasticsearch, encryption, hashing, io, json, profiling, testkit, time)

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.typesafeRepo("snapshots"),
    "Spray Repository"              at "https://repo.spray.io/",
    "Bintray Scalaz Releases"       at "https://dl.bintray.com/scalaz/releases",
    "JCenter Repository"            at "https://jcenter.bintray.com/"),

  scalariformPreferences := scalariformPreferences.value
    .setPreference(DanglingCloseParenthesis, Prevent)
    .setPreference(DoubleIndentConstructorArguments, true),

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
  releaseStepCommandAndRemaining("+test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  setNextVersion,
  commitNextVersion,
  pushChanges,
  releaseStepCommandAndRemaining("sonatypeReleaseAll"))
