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
      libraryDependencies <++= scalaVersion(commonLibraries(_)),
      resolvers += "rojoma.com" at "http://rjmac.github.com/maven/releases/"
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
