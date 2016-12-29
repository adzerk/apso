import BuildSettings._

shellPrompt in ThisBuild := { s => Project.extract(s).currentProject.id + " > " }

lazy val apso = project.in(file("apso"))
  .dependsOn(apsoTestkit % "test")
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "com.amazonaws"                  % "aws-java-sdk-ec2"          % "1.11.38"        % "provided",
    "com.amazonaws"                  % "aws-java-sdk-s3"           % "1.11.38"        % "provided",
    "com.chuusai"                   %% "shapeless"                 % "2.3.2",
    "com.github.nscala-time"        %% "nscala-time"               % "2.14.0",
    "com.hierynomus"                 % "sshj"                      % "0.17.2",
    "com.j256.simplejmx"             % "simplejmx"                 % "1.12",
    "com.jcraft"                     % "jzlib"                     % "1.1.3",
    "com.twmacinta"                  % "fast-md5"                  % "2.7.1",
    "com.typesafe"                   % "config"                    % "1.3.1"          % "provided",
    "com.typesafe.akka"             %% "akka-actor"                % "2.4.10"         % "provided",
    "com.typesafe.akka"             %% "akka-http-experimental"    % "2.4.10"         % "provided",
    "io.github.andrebeat"           %% "scala-pool"                % "0.3.0",
    "io.spray"                      %% "spray-can"                 % "1.3.3"          % "provided",
    "io.spray"                      %% "spray-httpx"               % "1.3.3"          % "provided",
    "io.spray"                      %% "spray-json"                % "1.3.2"          % "provided",
    "io.spray"                      %% "spray-routing-shapeless2"  % "1.3.3"          % "provided",
    "net.databinder.dispatch"       %% "dispatch-core"             % "0.11.3",
    "org.bouncycastle"               % "bcpkix-jdk15on"            % "1.55",
    "org.bouncycastle"               % "bcprov-jdk15on"            % "1.55",
    "org.scalaz"                    %% "scalaz-core"               % "7.2.6"          % "provided",
    "org.slf4j"                      % "slf4j-api"                 % "1.7.21",
    "com.typesafe.akka"             %% "akka-http-testkit"         % "2.4.10"         % "test",
    "io.spray"                      %% "spray-testkit"             % "1.3.3"          % "test",
    "junit"                          % "junit"                     % "4.12"           % "test",
    "org.scalacheck"                %% "scalacheck"                % "1.13.2"         % "test",
    "org.specs2"                    %% "specs2-core"               % "3.8.5"          % "test",
    "org.specs2"                    %% "specs2-scalacheck"         % "3.8.5"          % "test",
    "org.specs2"                    %% "specs2-junit"              % "3.8.5"          % "test"))

lazy val apsoTestkit = Project("apso-testkit", file("apso-testkit"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.akka"             %% "akka-testkit"       % "2.4.10"         % "provided",
    "com.typesafe.akka"             %% "akka-http-testkit"  % "2.4.10"         % "provided",
    "org.slf4j"                      % "slf4j-api"          % "1.7.21",
    "org.specs2"                    %% "specs2-core"        % "3.8.5"          % "provided",
    "org.specs2"                    %% "specs2-junit"       % "3.8.5"          % "provided"
  ))

lazy val root = project.in(file("."))
  .aggregate(apso, apsoTestkit)
  .settings(commonSettings: _*)
  .settings(noPublishing: _*)
