import BuildSettings._

shellPrompt in ThisBuild := { s => Project.extract(s).currentProject.id + " > " }

lazy val apso = project.in(file("apso"))
  .dependsOn(apsoTestkit % "test")
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(libraryDependencies ++= Seq(
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
    "org.specs2"                                %% "specs2-junit"                   % "3.8.6"          % "test"))
  .settings(libraryDependencies ++= {
    scalaBinaryVersion.value match {
      case "2.11" => Seq(
        "io.spray"                              %% "spray-can"                      % "1.3.4"          % "provided",
        "io.spray"                              %% "spray-httpx"                    % "1.3.4"          % "provided",
        "io.spray"                              %% "spray-routing-shapeless23"      % "1.3.4"          % "provided",
        "io.spray"                              %% "spray-testkit"                  % "1.3.4"          % "test")

      case _ => Nil
    }
  })

lazy val apsoTestkit = Project("apso-testkit", file("apso-testkit"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.akka"             %% "akka-testkit"       % "2.4.16"         % "provided",
    "com.typesafe.akka"             %% "akka-http-testkit"  % "10.0.1"         % "provided",
    "org.slf4j"                      % "slf4j-api"          % "1.7.22",
    "org.specs2"                    %% "specs2-core"        % "3.8.6"          % "provided",
    "org.specs2"                    %% "specs2-junit"       % "3.8.6"          % "provided"))

lazy val root = project.in(file("."))
  .aggregate(apso, apsoTestkit)
  .settings(commonSettings: _*)
  .settings(noPublishing: _*)
