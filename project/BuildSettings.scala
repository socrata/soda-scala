import sbt._
import Keys._


import Dependencies._

object BuildSettings {
  val buildSettings: Seq[Setting[_]] = Defaults.defaultSettings ++
    Seq(
      version := "2.0.5",
      scalaVersion := "2.10.4",
      crossScalaVersions := Seq(scalaVersion.value, "2.11.8", "2.12.0"),
      organization := "com.socrata",
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
      )
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
