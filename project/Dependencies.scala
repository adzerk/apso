import sbt._

object Dependencies {
  object Versions {
    val Akka                    = "2.6.0"
    val AkkaHttp                = "10.1.11"
    val AkkaTestkitSpecs2       = "0.2.3"
    val Aws                     = "1.11.553"
    val BouncyCastle            = "1.60"
    val Circe                   = "0.12.3"
    val CommonsCodec            = "1.12"
    val ConcurrentLinkedHashMap = "1.4.2"
    val Elastic4s               = "7.1.2"
    val FastMd5                 = "2.7.1"
    val JUnit                   = "4.12"
    val Log4jCore               = "2.11.2"
    val Log4jApiScala           = "11.0"
    val Netty                   = "4.1.43.Final"
    val NscalaTime              = "2.22.0"
    val ScalaCheck              = "1.14.2"
    val ScalaPool               = "0.4.1"
    val SimpleJmx               = "1.17"
    val Specs2                  = "4.8.1"
    val SprayJson               = "1.3.5"
    val Squants                 = "1.5.0"
    val SshJ                    = "0.27.0"
    val TypesafeConfig          = "1.4.0"
    val UnirestJava             = "1.4.9"
  }

  val AkkaActor                  = "com.typesafe.akka"                      %% "akka-actor"                   % Versions.Akka
  val AkkaHttp                   = "com.typesafe.akka"                      %% "akka-http"                    % Versions.AkkaHttp
  val AkkaHttpTestkit            = "com.typesafe.akka"                      %% "akka-http-testkit"            % Versions.AkkaHttp
  val AkkaStream                 = "com.typesafe.akka"                      %% "akka-stream"                  % Versions.Akka
  val AkkaStreamTestkit          = "com.typesafe.akka"                      %% "akka-stream-testkit"          % Versions.Akka
  val AkkaTestkit                = "com.typesafe.akka"                      %% "akka-testkit"                 % Versions.Akka
  val AkkaTestkitSpecs2          = "net.ruippeixotog"                       %% "akka-testkit-specs2"          % Versions.AkkaTestkitSpecs2
  val AwsJavaSdkS3               = "com.amazonaws"                           % "aws-java-sdk-s3"              % Versions.Aws
  val BouncyCastlePkix           = "org.bouncycastle"                        % "bcpkix-jdk15on"               % Versions.BouncyCastle
  val BouncyCastleProvider       = "org.bouncycastle"                        % "bcprov-jdk15on"               % Versions.BouncyCastle
  val CirceCore                  = "io.circe"                               %% "circe-core"                   % Versions.Circe
  val CirceGeneric               = "io.circe"                               %% "circe-generic"                % Versions.Circe
  val CirceLiteral               = "io.circe"                               %% "circe-literal"                % Versions.Circe
  val CirceParser                = "io.circe"                               %% "circe-parser"                 % Versions.Circe
  val CommonsCodec               = "commons-codec"                           % "commons-codec"                % Versions.CommonsCodec
  val ConcurrentLinkedHashMapLru = "com.googlecode.concurrentlinkedhashmap"  % "concurrentlinkedhashmap-lru"  % Versions.ConcurrentLinkedHashMap
  val Elastic4sClientEsJava      = "com.sksamuel.elastic4s"                 %% "elastic4s-client-esjava"      % Versions.Elastic4s
  val Elastic4sCore              = "com.sksamuel.elastic4s"                 %% "elastic4s-core"               % Versions.Elastic4s
  val Elastic4sTestkit           = "com.sksamuel.elastic4s"                 %% "elastic4s-testkit"            % Versions.Elastic4s
  val ElasticsearchClusterRunner = "org.codelibs"                            % "elasticsearch-cluster-runner" % "7.1.1.0" excludeAll ExclusionRule(organization = "io.netty")
  val FastMd5                    = "com.joyent.util"                         % "fast-md5"                     % Versions.FastMd5
  val JUnit                      = "junit"                                   % "junit"                        % Versions.JUnit
  val Log4jApiScala              = "org.apache.logging.log4j"               %% "log4j-api-scala"              % Versions.Log4jApiScala
  val Log4jCore                  = "org.apache.logging.log4j"                % "log4j-core"                   % Versions.Log4jCore
  val Netty                      = "io.netty"                                % "netty-all"                    % Versions.Netty
  val NscalaTime                 = "com.github.nscala-time"                 %% "nscala-time"                  % Versions.NscalaTime
  val ScalaCheck                 = "org.scalacheck"                         %% "scalacheck"                   % Versions.ScalaCheck
  val ScalaPool                  = "io.github.andrebeat"                    %% "scala-pool"                   % Versions.ScalaPool
  val SimpleJmx                  = "com.j256.simplejmx"                      % "simplejmx"                    % Versions.SimpleJmx
  val Specs2Core                 = "org.specs2"                             %% "specs2-core"                  % Versions.Specs2
  val Specs2JUnit                = "org.specs2"                             %% "specs2-junit"                 % Versions.Specs2
  val Specs2ScalaCheck           = "org.specs2"                             %% "specs2-scalacheck"            % Versions.Specs2
  val SprayJson                  = "io.spray"                               %% "spray-json"                   % Versions.SprayJson
  val Squants                    = "org.typelevel"                          %% "squants"                      % Versions.Squants
  val SshJ                       = "com.hierynomus"                          % "sshj"                         % Versions.SshJ
  val TypesafeConfig             = "com.typesafe"                            % "config"                       % Versions.TypesafeConfig
  val UnirestJava                = "com.mashape.unirest"                     % "unirest-java"                 % Versions.UnirestJava
}
