import sbt._
import Keys._

import sbtassembly.Plugin._
import AssemblyKeys._

import Dependencies._

object BuildSettings {
  val buildSettings: Seq[Setting[_]] = Defaults.defaultSettings ++ Seq(
    organization := "com.socrata",
    version := "1.0.0",
    scalaVersion := "2.9.2",
    crossScalaVersions := Seq("2.8.1", "2.8.2", "2.9.0", "2.9.0-1", "2.9.1", "2.9.1-1", "2.9.2")
  )

  def commonProjectSettings(assembly: Boolean = false): Seq[Setting[_]] =
    buildSettings ++
    (if(assembly) commonAssemblySettings else Seq.empty) ++
    Seq(
      scalacOptions <++= scalaVersion map compilerFlags,
      testOptions in Test ++= Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-oD")
      ),
      libraryDependencies <++= scalaVersion(commonLibraries(_))
    )

  def sonatypeSettings: Seq[Setting[_]] = Seq(
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if(v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    // random stuff Sonatype wants
    pomExtra := (
      <url>http://www.github.com/socrata/soda-scala</url>
      <licenses>
        <license>
          <name>The MIT License (MIT)</name>
          <url>http://opensource.org/licenses/mit-license.php</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git://github.com/socrata/soda-scala.git</url>
        <connection>scm:git://github.com/socrata/soda-scala.git</connection>
      </scm>
      <developers>
        <developer>
          <name>Robert Macomber</name>
          <email>robert.macomber@socrata.com</email>
          <organization>Socrata</organization>
        </developer>
      </developers>
    ),
    // random stuff Sonatype does not want
    pomIncludeRepository := { _ => false },
    // defaults, but set them anyway just in acse
    publishMavenStyle := true,
    publishArtifact in Test := false
  )

  def compilerFlags(sv: String) = ScalaVersion.v(sv) match {
    case Scala28 => Seq("-encoding", "UTF-8", "-g", "-unchecked", "-deprecation")
    case Scala29 => Seq("-encoding", "UTF-8", "-g:vars", "-unchecked", "-deprecation")
  }

  def commonLibraries(implicit scalaVersion: String) = Seq(
    slf4j,
    scalaTest % "test",
    slf4jSimple % "test"
  )

  def commonAssemblySettings: Seq[Setting[_]] = assemblySettings ++ Seq(
    test in assembly := {}
  )
}
