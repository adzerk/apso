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
    .dependsOn(apsoTestkit % "test")
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(apsoSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.amazonaws"                  % "aws-java-sdk-ec2"          % "1.11.27"        % "provided",
      "com.amazonaws"                  % "aws-java-sdk-s3"           % "1.11.27"        % "provided",
      "com.chuusai"                   %% "shapeless"                 % "2.3.1",
      "com.github.melrief"            %% "pureconfig"                % "0.2.0",
      "com.github.nscala-time"        %% "nscala-time"               % "2.12.0"         % "provided",
      "com.hierynomus"                 % "sshj"                      % "0.17.2",
      "com.j256.simplejmx"             % "simplejmx"                 % "1.12",
      "com.jcraft"                     % "jzlib"                     % "1.1.3",
      "com.twmacinta"                  % "fast-md5"                  % "2.7.1",
      "com.typesafe"                   % "config"                    % "1.3.0"          % "provided",
      "com.typesafe.akka"             %% "akka-actor"                % "2.4.8"          % "provided",
      "io.github.andrebeat"           %% "scala-pool"                % "0.3.0",
      "io.spray"                      %% "spray-can"                 % "1.3.3"          % "provided",
      "io.spray"                      %% "spray-httpx"               % "1.3.3"          % "provided",
      "io.spray"                      %% "spray-json"                % "1.3.2"          % "provided",
      "io.spray"                      %% "spray-routing-shapeless2"  % "1.3.3"          % "provided",
      "net.databinder.dispatch"       %% "dispatch-core"             % "0.11.3",
      "org.bouncycastle"               % "bcpkix-jdk15on"            % "1.54",
      "org.bouncycastle"               % "bcprov-jdk15on"            % "1.54",
      "org.scalaz"                    %% "scalaz-core"               % "7.2.5"          % "provided",
      "org.slf4j"                      % "slf4j-api"                 % "1.7.21",
      "io.spray"                      %% "spray-testkit"             % "1.3.3"          % "test",
      "org.specs2"                    %% "specs2-core"               % "3.8.4"          % "test",
      "org.specs2"                    %% "specs2-scalacheck"         % "3.8.4"          % "test",
      "org.specs2"                    %% "specs2-junit"              % "3.8.4"          % "test",
      "junit"                          % "junit"                     % "4.12"           % "test",
      "org.scalacheck"                %% "scalacheck"                % "1.13.2"         % "test"))

  lazy val apsoTestkit = Project("apso-testkit", file("apso-testkit"))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(apsoTestkitSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.akka"             %% "akka-testkit"       % "2.4.8"          % "provided",
      "org.slf4j"                      % "slf4j-api"          % "1.7.21",
      "org.specs2"                    %% "specs2-core"        % "3.8.4"          % "provided",
      "org.specs2"                    %% "specs2-junit"       % "3.8.4"          % "provided"
  ))

  lazy val commonSettings = Defaults.coreDefaultSettings ++ formatSettings ++ Seq(
    organization := "eu.shiftforward",
    version := "0.8-SNAPSHOT",
    scalaVersion := "2.11.8",

    resolvers ++= Seq(
      "Sonatype Repository"           at "http://oss.sonatype.org/content/repositories/releases",
      "Sonatype Snapshots Repository" at "http://oss.sonatype.org/content/repositories/snapshots",
      "Spray Repository"              at "http://repo.spray.io/",
      "Bintray Scalaz Releases"       at "http://dl.bintray.com/scalaz/releases",
      "Typesafe Repository"           at "http://repo.typesafe.com/typesafe/releases/",
      "JCenter Repository"            at "http://jcenter.bintray.com/",
      "JAnalyse Repository"           at "http://www.janalyse.fr/repository/"),

    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console"),

    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:reflectiveCalls"))

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
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false },
    licenses := Seq("Apache License, Version 2.0" ->
      url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("https://github.com/ShiftForward/apso")),
    pomExtra := (
      <scm>
        <url>https://github.com/ShiftForward/apso.git</url>
        <connection>scm:git:git@github.com:ShiftForward/apso.git</connection>
      </scm>
      <developers>
        <developer>
          <id>ruippeixotog</id>
          <name>Rui Gonçalves</name>
          <roles>
            <role>developer</role>
          </roles>
        </developer>
        <developer>
          <id>andrebeat</id>
          <name>André Silva</name>
          <roles>
            <role>developer</role>
          </roles>
        </developer>
        <developer>
          <id>jcazevedo</id>
          <name>Joao Azevedo</name>
          <roles>
            <role>developer</role>
          </roles>
        </developer>
        <developer>
          <id>JD557</id>
          <name>João Costa</name>
          <roles>
            <role>developer</role>
          </roles>
        </developer>
        <developer>
          <id>queimadus</id>
          <name>Bruno Maia</name>
          <roles>
            <role>developer</role>
          </roles>
        </developer>
        <developer>
          <id>luismfonseca</id>
          <name>Luís Fonseca</name>
          <roles>
            <role>developer</role>
          </roles>
        </developer>
        <developer>
          <id>hugoferreira</id>
          <name>Hugo Ferreira</name>
          <roles>
            <role>developer</role>
          </roles>
        </developer>
        <developer>
          <id>belerophon</id>
          <name>André Cardoso</name>
          <roles>
            <role>developer</role>
          </roles>
        </developer>
      </developers>))

  lazy val noPublishing = Seq(
    publish := (),
    publishLocal := ()
  )

  // configure prompt to show current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
  }
}
