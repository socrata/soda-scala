import sbt._

object Dependencies {
  object versions {
    val scalaCheck_28 = "1.8"
    val scalaCheck_29 = "1.10.0"
    val scalaTest = "1.8"
    val slf4j = "1.6.6"
  }

  def scalaCheck(implicit scalaVersion: String) = ScalaVersion.v match {
    case Scala28 => "org.scalacheck" % "scalacheck_2.8.1" % versions.scalaCheck_28
    case Scala29 => "org.scalacheck" %% "scalacheck" % versions.scalaCheck_29
  }

  val scalaTest = "org.scalatest" %% "scalatest" % versions.scalaTest

  val slf4j = "org.slf4j" % "slf4j-api" % versions.slf4j
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % versions.slf4j
}
