import com.typesafe.sbt.SbtScalariform.autoImport._
import sbt.Keys._
import sbt._
import scalariform.formatter.preferences._

object BuildSettings {

  lazy val formatSettings = Seq(
    scalariformPreferences := scalariformPreferences.value
      .setPreference(DanglingCloseParenthesis, Prevent)
      .setPreference(DoubleIndentClassDeclaration, true))

  lazy val commonSettings = Defaults.coreDefaultSettings ++ formatSettings ++ Seq(
    organization := "eu.shiftforward",
    version := "0.10.0",
    scalaVersion := "2.12.1",
    crossScalaVersions := List("2.11.8", "2.12.1"),

    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots"),
      Resolver.typesafeRepo("releases"),
      Resolver.typesafeRepo("snapshots"),
      "Spray Repository"              at "http://repo.spray.io/",
      "Bintray Scalaz Releases"       at "http://dl.bintray.com/scalaz/releases",
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


  lazy val publishSettings = Seq(
    publishTo := {
      val sf = "http://nexus.int.shiftforward.eu/content/repositories/"
      if (isSnapshot.value) Some("snapshots" at sf + "snapshots")
      else Some("releases"  at sf + "releases")
    },
    pomIncludeRepository := { _ => false },
    licenses := Seq("Apache License, Version 2.0" ->
      url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("https://github.com/ShiftForward/apso")),
    pomExtra := {
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
        </developers>
    })

  lazy val noPublishing = Seq(
    publish := (),
    publishLocal := ())
}
