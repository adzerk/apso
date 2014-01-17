import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

object ProjectBuild extends Build {

  lazy val root = Project("root", file("."))
    .aggregate(apso, apsoTestkit)
    .settings(commonSettings: _*)
    .settings(noPublishing: _*)

  lazy val apso = Project("apso", file("apso"))
    .dependsOn(apsoTestkit % "provided")
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(apsoSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.amazonaws"                  % "aws-java-sdk"       % "1.6.12"         % "provided",
      "com.github.nscala-time"        %% "nscala-time"        % "0.6.0"          % "provided",
      "com.typesafe.akka"             %% "akka-actor"         % "2.2.3"          % "provided",
      "com.twmacinta"                  % "fast-md5"           % "2.7.1",
      "io.spray"                      %% "spray-json"         % "1.2.5"          % "provided",
      "io.spray"                       % "spray-httpx"        % "1.2.0"          % "provided",
      "org.scalaz"                    %% "scalaz-core"        % "7.0.5"          % "provided",
      "org.slf4j"                      % "slf4j-api"          % "1.7.5",
      "org.specs2"                    %% "specs2"             % "2.2.3"          % "test",
      "junit"                          % "junit"              % "4.11"           % "test"
    ))

  lazy val apsoTestkit = Project("apso-testkit", file("apso-testkit"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(apsoTestkitSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "org.specs2"                    %% "specs2"             % "2.2.3"
    ))

  lazy val commonSettings = Project.defaultSettings ++ formatSettings ++ Seq(
    organization := "eu.shiftforward",
    version := "0.3-SNAPSHOT",
    scalaVersion := "2.10.3",

    resolvers ++= Seq(
      "Sonatype Repository"           at "http://oss.sonatype.org/content/repositories/releases",
      "Sonatype Snapshots Repository" at "http://oss.sonatype.org/content/repositories/snapshots",
      "Spray Repository"              at "http://repo.spray.io/",
      "Typesafe Repository"           at "http://repo.typesafe.com/typesafe/releases/",
      "SF Nexus Releases"             at "http://NEXUS_URL/content/repositories/releases",
      "SF Nexus Snapshots"            at "http://NEXUS_URL/content/repositories/snapshots",
      "3rd Party"                     at "http://NEXUS_URL/content/repositories/thirdparty",
      "3rd Party Snapshots"           at "http://NEXUS_URL/content/repositories/thirdparty-snapshots"),

    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console"),

    scalacOptions ++= Seq("-deprecation", "-unchecked"))

  lazy val apsoSettings = Nil

  lazy val apsoTestkitSettings = Nil

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test := formattingPreferences)

  def formattingPreferences =
    FormattingPreferences()
      .setPreference(AlignParameters, true)
      .setPreference(DoubleIndentClassDeclaration, true)

  lazy val publishSettings = Seq(
    publishTo <<= version { (v: String) =>
      val sf = "http://NEXUS_URL/content/repositories/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at sf + "snapshots")
      else
        Some("releases"  at sf + "releases")
    },

    credentials += Credentials(
      "Sonatype Nexus Repository Manager",
      "NEXUS_URL",
      "NEXUS_USER",
      "NEXUS_PASS")
  )

  lazy val noPublishing = Seq(
    publish := (),
    publishLocal := ()
  )

  // configure prompt to show current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
  }
}
