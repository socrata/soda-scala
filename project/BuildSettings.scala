import sbt._
import Keys._

import Dependencies._

object BuildSettings {
  val buildSettings: Seq[Setting[_]] = Defaults.defaultSettings ++
    Seq(
      version := "2.0.0-SNAPSHOT",
      scalaVersion := "2.10.1",
      crossScalaVersions := Seq("2.9.3", "2.10.1")
    )

  def commonProjectSettings(assembly: Boolean = false): Seq[Setting[_]] =
    buildSettings ++
    Seq(
      testOptions in Test ++= Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-oD")
      ),
      libraryDependencies <++= scalaVersion(commonLibraries(_))
    )

  def commonLibraries(implicit scalaVersion: String) = Seq(
    slf4j,
    scalaTest % "test",
    slf4jSimple % "test"
  )
}
