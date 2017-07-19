import scalariform.formatter.preferences._

organization in ThisBuild := "eu.shiftforward"

scalaVersion in ThisBuild := "2.12.2"
crossScalaVersions in ThisBuild := Seq("2.11.11", "2.12.2")

lazy val core = project.in(file("core"))
  .dependsOn(testkit % "test")
  .settings(commonSettings: _*)
  .settings(
    name := "apso",
    libraryDependencies ++= Seq(
      "com.amazonaws"                              % "aws-java-sdk-ec2"               % "1.11.74"        % "provided",
      "com.amazonaws"                              % "aws-java-sdk-s3"                % "1.11.74"        % "provided",
      "com.chuusai"                               %% "shapeless"                      % "2.3.2",
      "com.github.nscala-time"                    %% "nscala-time"                    % "2.16.0",
      "com.googlecode.concurrentlinkedhashmap"     % "concurrentlinkedhashmap-lru"    % "1.4.2",
      "com.hierynomus"                             % "sshj"                           % "0.19.0",
      "com.j256.simplejmx"                         % "simplejmx"                      % "1.13",
      "com.jcraft"                                 % "jzlib"                          % "1.1.3",
      "com.mashape.unirest"                        % "unirest-java"                   % "1.4.9",
      "com.twmacinta"                              % "fast-md5"                       % "2.7.1",
      "com.typesafe"                               % "config"                         % "1.3.1"          % "provided",
      "com.typesafe.akka"                         %% "akka-actor"                     % "2.4.16"         % "provided",
      "com.typesafe.akka"                         %% "akka-http"                      % "10.0.1"         % "provided",
      "io.github.andrebeat"                       %% "scala-pool"                     % "0.4.0",
      "io.spray"                                  %% "spray-json"                     % "1.3.3"          % "provided",
      "commons-codec"                              % "commons-codec"                  % "1.10",
      "org.bouncycastle"                           % "bcpkix-jdk15on"                 % "1.56",
      "org.bouncycastle"                           % "bcprov-jdk15on"                 % "1.56",
      "org.scalaz"                                %% "scalaz-core"                    % "7.2.8"          % "provided",
      "org.slf4j"                                  % "slf4j-api"                      % "1.7.22",
      "com.typesafe.akka"                         %% "akka-http-testkit"              % "10.0.1"         % "test",
      "junit"                                      % "junit"                          % "4.12"           % "test",
      "org.scalacheck"                            %% "scalacheck"                     % "1.13.4"         % "test",
      "org.specs2"                                %% "specs2-core"                    % "3.8.6"          % "test",
      "org.specs2"                                %% "specs2-scalacheck"              % "3.8.6"          % "test",
      "org.specs2"                                %% "specs2-junit"                   % "3.8.6"          % "test"),

    libraryDependencies ++= (scalaBinaryVersion.value match {
      case "2.11" => Seq(
        "io.spray"                              %% "spray-can"                      % "1.3.4"          % "provided",
        "io.spray"                              %% "spray-httpx"                    % "1.3.4"          % "provided",
        "io.spray"                              %% "spray-routing-shapeless23"      % "1.3.4"          % "provided",
        "io.spray"                              %% "spray-testkit"                  % "1.3.4"          % "test")

      case _ => Nil
    }))

lazy val testkit = project.in(file("testkit"))
  .settings(commonSettings: _*)
  .settings(
    name := "apso-testkit",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"             %% "akka-testkit"       % "2.4.16"         % "provided",
      "com.typesafe.akka"             %% "akka-http-testkit"  % "10.0.1"         % "provided",
      "org.slf4j"                      % "slf4j-api"          % "1.7.22",
      "org.specs2"                    %% "specs2-core"        % "3.8.6"          % "provided",
      "org.specs2"                    %% "specs2-junit"       % "3.8.6"          % "provided"))

lazy val commonScalacOptions = {
  val allVersionScalacOptions = Seq(
    "-encoding", "UTF-8",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Ywarn-unused-import")

  val scala211ScalacOptions = allVersionScalacOptions ++ Seq(
    "-Xlint:-adapted-args,-nullary-override,_")

  // Scala 2.12.2 has excessive warnings about unused implicits. See https://github.com/scala/bug/issues/10270
  val scala212ScalacOptions = allVersionScalacOptions ++ Seq(
    "-Xlint:-unused,-nullary-override,-adapted-args,_",
    "-Ywarn-unused:-params")

  Def.task {
    scalacOptions.value ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) => scala212ScalacOptions
      case Some((2, 11)) => scala211ScalacOptions
      case _ => Nil // for the aggregation project
    })
  }
}

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.typesafeRepo("snapshots"),
    "Spray Repository"              at "http://repo.spray.io/",
    "Bintray Scalaz Releases"       at "http://dl.bintray.com/scalaz/releases",
    "JCenter Repository"            at "http://jcenter.bintray.com/",
    "JAnalyse Repository"           at "http://www.janalyse.fr/repository/"),

  scalariformPreferences := scalariformPreferences.value
    .setPreference(DanglingCloseParenthesis, Prevent)
    .setPreference(DoubleIndentClassDeclaration, true),

  scalacOptions := commonScalacOptions.value,

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },

  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },

  licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/ShiftForward/apso")),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value)

releaseProcess := releaseProcess.value ++ Seq[ReleaseStep](
  releaseStepCommandAndRemaining("sonatypeReleaseAll"))

releaseCrossBuild := true
releaseTagComment := s"Release ${(version in ThisBuild).value}"
releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}"
