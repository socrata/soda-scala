import sbt._
import Keys._

import Dependencies._

object SodaSample {
  lazy val settings: Seq[Setting[_]] = BuildSettings.commonProjectSettings(assembly = true) ++ BuildSettings.sonatypeSettings ++ Seq(
    libraryDependencies <++= scalaVersion(libraries(_)),
    publish := {}
  )

  def libraries(implicit scalaVersion: String) = Seq(
    rojomaJson,
    slf4jSimple
  )
}
