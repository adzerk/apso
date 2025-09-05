import sbt._

object Dependencies {
  object Versions {
    val Scala213 = "2.13.16"
    val Scala3   = "3.3.6"

    val Pekko          = "1.2.0"
    val PekkoHttp      = "1.2.0"
    val Aws            = "1.12.790"
    val BouncyCastle   = "1.81"
    val Cats           = "2.13.0"
    val Circe          = "0.14.14"
    val CommonsCodec   = "1.19.0"
    val FastMd5        = "2.7.1"
    val JUnit          = "4.13.2"
    val JodaTime       = "2.14.0"
    val ScalaCheck     = "1.18.1"
    val ScalaLogging   = "3.9.5"
    val ScalaPool      = "0.5.0"
    val ScalaTest      = "3.2.19"
    val SimpleJmx      = "2.2"
    val Specs2_4       = "4.21.0"
    val Specs2_5       = "5.6.4"
    val Squants        = "1.8.3"
    val SshJ           = "0.40.0"
    val TypesafeConfig = "1.4.4"
    val UnirestJava    = "4.5.1"
  }

  // scalafmt: { maxColumn = 200 }
  val AwsJavaSdkCore         = "com.amazonaws"               % "aws-java-sdk-core"         % Versions.Aws
  val AwsJavaSdkS3           = "com.amazonaws"               % "aws-java-sdk-s3"           % Versions.Aws
  val BouncyCastlePkix       = "org.bouncycastle"            % "bcpkix-jdk18on"            % Versions.BouncyCastle
  val BouncyCastleProvider   = "org.bouncycastle"            % "bcprov-jdk18on"            % Versions.BouncyCastle
  val CatsCore               = "org.typelevel"              %% "cats-core"                 % Versions.Cats
  val CirceCore              = "io.circe"                   %% "circe-core"                % Versions.Circe
  val CirceGeneric           = "io.circe"                   %% "circe-generic"             % Versions.Circe
  val CirceLiteral           = "io.circe"                   %% "circe-literal"             % Versions.Circe
  val CirceParser            = "io.circe"                   %% "circe-parser"              % Versions.Circe
  val CommonsCodec           = "commons-codec"               % "commons-codec"             % Versions.CommonsCodec
  val FastMd5                = "com.joyent.util"             % "fast-md5"                  % Versions.FastMd5
  val JUnit                  = "junit"                       % "junit"                     % Versions.JUnit
  val JodaTime               = "joda-time"                   % "joda-time"                 % Versions.JodaTime
  val PekkoActor             = "org.apache.pekko"           %% "pekko-actor"               % Versions.Pekko
  val PekkoActorTestkitTyped = "org.apache.pekko"           %% "pekko-actor-testkit-typed" % Versions.Pekko
  val PekkoActorTyped        = "org.apache.pekko"           %% "pekko-actor-typed"         % Versions.Pekko
  val PekkoHttp              = "org.apache.pekko"           %% "pekko-http"                % Versions.PekkoHttp
  val PekkoHttpCore          = "org.apache.pekko"           %% "pekko-http-core"           % Versions.PekkoHttp
  val PekkoHttpTestkit       = "org.apache.pekko"           %% "pekko-http-testkit"        % Versions.PekkoHttp
  val PekkoStream            = "org.apache.pekko"           %% "pekko-stream"              % Versions.Pekko
  val PekkoStreamTestkit     = "org.apache.pekko"           %% "pekko-stream-testkit"      % Versions.Pekko
  val Scaffeine              = "com.github.blemale"         %% "scaffeine"                 % "5.3.0"
  val ScalaCheck             = "org.scalacheck"             %% "scalacheck"                % Versions.ScalaCheck
  val ScalaLogging           = "com.typesafe.scala-logging" %% "scala-logging"             % Versions.ScalaLogging
  val ScalaPool              = "io.github.andrebeat"        %% "scala-pool"                % Versions.ScalaPool
  val ScalaTestCore          = "org.scalatest"              %% "scalatest-core"            % Versions.ScalaTest
  val SimpleJmx              = "com.j256.simplejmx"          % "simplejmx"                 % Versions.SimpleJmx
  val Specs2_4Common         = "org.specs2"                 %% "specs2-common"             % Versions.Specs2_4
  val Specs2_4Core           = "org.specs2"                 %% "specs2-core"               % Versions.Specs2_4
  val Specs2_4JUnit          = "org.specs2"                 %% "specs2-junit"              % Versions.Specs2_4
  val Specs2_4Matcher        = "org.specs2"                 %% "specs2-matcher"            % Versions.Specs2_4
  val Specs2_4ScalaCheck     = "org.specs2"                 %% "specs2-scalacheck"         % Versions.Specs2_4
  val Specs2_5Common         = "org.specs2"                 %% "specs2-common"             % Versions.Specs2_5
  val Specs2_5Core           = "org.specs2"                 %% "specs2-core"               % Versions.Specs2_5
  val Specs2_5Matcher        = "org.specs2"                 %% "specs2-matcher"            % Versions.Specs2_5
  val Squants                = "org.typelevel"              %% "squants"                   % Versions.Squants
  val SshJ                   = "com.hierynomus"              % "sshj"                      % Versions.SshJ
  val TypesafeConfig         = "com.typesafe"                % "config"                    % Versions.TypesafeConfig
  val UnirestJava            = "com.konghq"                  % "unirest-java-core"         % Versions.UnirestJava
  // scalafmt: { maxColumn = 120 }
}
