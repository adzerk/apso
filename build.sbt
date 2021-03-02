import scalariform.formatter.preferences._
import ReleaseTransformations._

organization in ThisBuild := "com.velocidi"

crossScalaVersions in ThisBuild := Seq("2.12.12", "2.13.4")
scalaVersion in ThisBuild := "2.12.12"

def module(project: Project, moduleName: String) =
  (project in file(moduleName))
    .settings(name := s"apso-$moduleName")
    .settings(commonSettings: _*)

lazy val akkaHttp      = module(project, "akka-http").dependsOn(log, core % Test, testkit % Test)
lazy val aws           = module(project, "aws").dependsOn(core, log)
lazy val caching       = module(project, "caching")
lazy val collections   = module(project, "collections")
lazy val core          = module(project, "core").dependsOn(testkit % Test)
lazy val elasticsearch = module(project, "elasticsearch").dependsOn(log, testkit % Test)
lazy val encryption    = module(project, "encryption").dependsOn(log)
lazy val hashing       = module(project, "hashing")
lazy val io            = module(project, "io").dependsOn(aws, testkit % Test)
lazy val json          = module(project, "json")
lazy val log           = module(project, "log")
lazy val profiling     = module(project, "profiling").dependsOn(core, log)
lazy val testkit       = module(project, "testkit")
lazy val time          = module(project, "time")

lazy val apso = (project in file("."))
  .settings(commonSettings: _*)
  .settings(name := "apso")
  .dependsOn(akkaHttp, aws, caching, collections, core, elasticsearch, encryption, hashing, io, json, log, profiling, time)
  .aggregate(akkaHttp, aws, caching, collections, core, elasticsearch, encryption, hashing, io, json, log, profiling, testkit, time)

lazy val docs = (project in file("apso-docs"))
  .dependsOn(apso)
  .settings(commonSettings: _*)
  .settings(
    mdocOut := baseDirectory.in(ThisBuild).value,

    mdocVariables := Map(
      "VERSION" -> "0.16.3" // This version should be set to the currently released version.
    ),

    // This is necessary because `aws-java-sdk-s3` has the `provided` scope in apso-aws
    libraryDependencies ++= Seq(Dependencies.AwsJavaSdkS3),

    skip in publish := true
  )
  .enablePlugins(MdocPlugin)

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

  publishTo := sonatypePublishToBundle.value,

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

releaseCrossBuild := true
releaseTagComment := s"Release ${(version in ThisBuild).value}"
releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}"

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
  pushChanges)
