import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

object ProjectBuild extends Build {
  lazy val project = "apso"

  lazy val root = Project(id = project,
                          base = file("."),
                          settings = Project.defaultSettings ++ formatSettings)
                            .settings(
    organization := "eu.shiftforward",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.10.2",

    publishSetting,
    credentialsSetting,

    resolvers ++= Seq(
      "Sonatype Repository"           at "http://oss.sonatype.org/content/repositories/releases",
      "Sonatype Snapshots Repository" at "http://oss.sonatype.org/content/repositories/snapshots",
      "Spray Repository"              at "http://repo.spray.io/",
      "Typesafe Repository"           at "http://repo.typesafe.com/typesafe/releases/",
      "SF Nexus Releases"             at "http://NEXUS_URL/content/repositories/releases",
      "SF Nexus Snapshots"            at "http://NEXUS_URL/content/repositories/snapshots",
      "3rd Party"                     at "http://NEXUS_URL/content/repositories/thirdparty",
      "3rd Party Snapshots"           at "http://NEXUS_URL/content/repositories/thirdparty-snapshots"
    ),

    libraryDependencies ++= Seq(
      "com.github.nscala-time"        %% "nscala-time"        % "0.4.2"  % "provided",
      "com.typesafe.akka"             %% "akka-actor"         % "2.1.4"  % "provided",
      "com.twmacinta"                  % "fast-md5"           % "2.7.1",
      "io.spray"                      %% "spray-json"         % "1.2.3"  % "provided",
      "io.spray"                       % "spray-httpx"        % "1.1-M8" % "provided",
      "org.scalaz"                    %% "scalaz-core"        % "7.0.0"  % "provided",
      "org.specs2"                    %% "specs2"             % "1.14"   % "test",
      "junit"                          % "junit"              % "4.11"   % "test"
    ),

    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console"),

    scalacOptions ++= Seq("-deprecation", "-unchecked"),

    scalacOptions in Compile in doc ++= Opts.doc.title("apso"), // TODO define title using sbt keys
    scalacOptions in Compile in doc ++= Opts.doc.version("1.0-SNAPSHOT"), // TODO define project version using sbt keys
    scalacOptions in Compile in doc += "-external-urls:scala=http://www.scala-lang.org/api/2.10.2" // TODO define scala version using sbt keys
  )

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test := formattingPreferences
  )

  def formattingPreferences =
    FormattingPreferences()
      .setPreference(AlignParameters, true)
      .setPreference(CompactControlReadability, true)
      .setPreference(DoubleIndentClassDeclaration, true)

  lazy val publishSetting = publishTo <<= version { (v: String) =>
    val sf = "http://NEXUS_URL/content/repositories/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at sf + "snapshots")
    else
      Some("releases"  at sf + "releases")
  }

  lazy val credentialsSetting = credentials += Credentials(
    "Sonatype Nexus Repository Manager",
    "NEXUS_URL",
    "NEXUS_USER",
    "NEXUS_PASS")
}
