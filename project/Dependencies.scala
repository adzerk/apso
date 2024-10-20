import sbt._

object Dependencies {
  object Versions {
    val Akka                    = "2.6.20"
    val AkkaHttp                = "10.2.10"
    val Pekko                   = "1.1.2"
    val PekkoHttp               = "1.1.0"
    val Aws                     = "1.12.776"
    val BouncyCastle            = "1.78.1"
    val Cats                    = "2.12.0"
    val Circe                   = "0.14.10"
    val CommonsCodec            = "1.17.1"
    val ConcurrentLinkedHashMap = "1.4.2"
    val Elastic4s               = "7.16.3"
    val Elasticsearch           = "7.16.3"
    val FastMd5                 = "2.7.1"
    val JUnit                   = "4.13.2"
    val JodaTime                = "2.13.0"
    val Log4J                   = "2.24.1"
    val NscalaTime              = "2.34.0"
    val ScalaCheck              = "1.18.1"
    val ScalaLogging            = "3.9.5"
    val ScalaPool               = "0.4.3"
    val ScalaTest               = "3.2.19"
    val Shapeless               = "2.3.12"
    val SimpleJmx               = "2.2"
    val Specs2                  = "4.20.9"
    val Squants                 = "1.8.3"
    val SshJ                    = "0.39.0"
    val TypesafeConfig          = "1.4.3"
    val UnirestJava             = "4.4.4"
  }

  // scalafmt: { maxColumn = 200 }
  val AkkaActor                  = "com.typesafe.akka"                     %% "akka-actor"                   % Versions.Akka
  val AkkaActorTestkitTyped      = "com.typesafe.akka"                     %% "akka-actor-testkit-typed"     % Versions.Akka
  val AkkaActorTyped             = "com.typesafe.akka"                     %% "akka-actor-typed"             % Versions.Akka
  val AkkaHttp                   = "com.typesafe.akka"                     %% "akka-http"                    % Versions.AkkaHttp
  val AkkaHttpCore               = "com.typesafe.akka"                     %% "akka-http-core"               % Versions.AkkaHttp
  val AkkaHttpTestkit            = "com.typesafe.akka"                     %% "akka-http-testkit"            % Versions.AkkaHttp
  val AkkaSlf4J                  = "com.typesafe.akka"                     %% "akka-slf4j"                   % Versions.Akka
  val AkkaStream                 = "com.typesafe.akka"                     %% "akka-stream"                  % Versions.Akka
  val AkkaStreamTestkit          = "com.typesafe.akka"                     %% "akka-stream-testkit"          % Versions.Akka
  val AkkaTestkit                = "com.typesafe.akka"                     %% "akka-testkit"                 % Versions.Akka
  val ApacheHttpAsyncClient      = "org.apache.httpcomponents"              % "httpasyncclient"              % "4.1.5"
  val ApacheHttpClient           = "org.apache.httpcomponents"              % "httpclient"                   % "4.5.14"
  val ApacheHttpCore             = "org.apache.httpcomponents"              % "httpcore"                     % "4.4.16"
  val AwsJavaSdkCore             = "com.amazonaws"                          % "aws-java-sdk-core"            % Versions.Aws
  val AwsJavaSdkS3               = "com.amazonaws"                          % "aws-java-sdk-s3"              % Versions.Aws
  val BouncyCastlePkix           = "org.bouncycastle"                       % "bcpkix-jdk18on"               % Versions.BouncyCastle
  val BouncyCastleProvider       = "org.bouncycastle"                       % "bcprov-jdk18on"               % Versions.BouncyCastle
  val CatsCore                   = "org.typelevel"                         %% "cats-core"                    % Versions.Cats
  val CirceCore                  = "io.circe"                              %% "circe-core"                   % Versions.Circe
  val CirceGeneric               = "io.circe"                              %% "circe-generic"                % Versions.Circe
  val CirceLiteral               = "io.circe"                              %% "circe-literal"                % Versions.Circe
  val CirceParser                = "io.circe"                              %% "circe-parser"                 % Versions.Circe
  val CommonsCodec               = "commons-codec"                          % "commons-codec"                % Versions.CommonsCodec
  val ConcurrentLinkedHashMapLru = "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru"  % Versions.ConcurrentLinkedHashMap
  val Elastic4sClientEsJava      = "com.sksamuel.elastic4s"                %% "elastic4s-client-esjava"      % Versions.Elastic4s
  val Elastic4sCore              = "com.sksamuel.elastic4s"                %% "elastic4s-core"               % Versions.Elastic4s
  val Elastic4sTestkit           = "com.sksamuel.elastic4s"                %% "elastic4s-testkit"            % Versions.Elastic4s
  val ElasticsearchClusterRunner = "org.codelibs"                           % "elasticsearch-cluster-runner" % "7.16.3.0"
  val ElasticsearchRestClient    = "org.elasticsearch.client"               % "elasticsearch-rest-client"    % Versions.Elasticsearch
  val FastMd5                    = "com.joyent.util"                        % "fast-md5"                     % Versions.FastMd5
  val JUnit                      = "junit"                                  % "junit"                        % Versions.JUnit
  val JodaTime                   = "joda-time"                              % "joda-time"                    % Versions.JodaTime
  val Log4JCore                  = "org.apache.logging.log4j"               % "log4j-core"                   % Versions.Log4J
  val Log4JSlf4j                 = "org.apache.logging.log4j"               % "log4j-slf4j-impl"             % Versions.Log4J
  val NscalaTime                 = "com.github.nscala-time"                %% "nscala-time"                  % Versions.NscalaTime
  val PekkoActor                 = "org.apache.pekko"                      %% "pekko-actor"                  % Versions.Pekko
  val PekkoActorTestkitTyped     = "org.apache.pekko"                      %% "pekko-actor-testkit-typed"    % Versions.Pekko
  val PekkoActorTyped            = "org.apache.pekko"                      %% "pekko-actor-typed"            % Versions.Pekko
  val PekkoHttp                  = "org.apache.pekko"                      %% "pekko-http"                   % Versions.PekkoHttp
  val PekkoHttpCore              = "org.apache.pekko"                      %% "pekko-http-core"              % Versions.PekkoHttp
  val PekkoHttpTestkit           = "org.apache.pekko"                      %% "pekko-http-testkit"           % Versions.PekkoHttp
  val PekkoSlf4J                 = "org.apache.pekko"                      %% "pekko-slf4j"                  % Versions.Pekko
  val PekkoStream                = "org.apache.pekko"                      %% "pekko-stream"                 % Versions.Pekko
  val PekkoStreamTestkit         = "org.apache.pekko"                      %% "pekko-stream-testkit"         % Versions.Pekko
  val PekkoTestkit               = "org.apache.pekko"                      %% "pekko-testkit"                % Versions.Pekko
  val ScalaCheck                 = "org.scalacheck"                        %% "scalacheck"                   % Versions.ScalaCheck
  val ScalaCollectionCompat      = "org.scala-lang.modules"                %% "scala-collection-compat"      % "2.12.0"
  val ScalaLogging               = "com.typesafe.scala-logging"            %% "scala-logging"                % Versions.ScalaLogging
  val ScalaPool                  = "io.github.andrebeat"                   %% "scala-pool"                   % Versions.ScalaPool
  val ScalaTestCore              = "org.scalatest"                         %% "scalatest-core"               % Versions.ScalaTest
  val Shapeless                  = "com.chuusai"                           %% "shapeless"                    % Versions.Shapeless
  val SimpleJmx                  = "com.j256.simplejmx"                     % "simplejmx"                    % Versions.SimpleJmx
  val Specs2Common               = "org.specs2"                            %% "specs2-common"                % Versions.Specs2
  val Specs2Core                 = "org.specs2"                            %% "specs2-core"                  % Versions.Specs2
  val Specs2JUnit                = "org.specs2"                            %% "specs2-junit"                 % Versions.Specs2
  val Specs2Matcher              = "org.specs2"                            %% "specs2-matcher"               % Versions.Specs2
  val Specs2ScalaCheck           = "org.specs2"                            %% "specs2-scalacheck"            % Versions.Specs2
  val Squants                    = "org.typelevel"                         %% "squants"                      % Versions.Squants
  val SshJ                       = "com.hierynomus"                         % "sshj"                         % Versions.SshJ
  val TypesafeConfig             = "com.typesafe"                           % "config"                       % Versions.TypesafeConfig
  val UnirestJava                = "com.konghq"                             % "unirest-java-core"            % Versions.UnirestJava
  // scalafmt: { maxColumn = 120 }
}
