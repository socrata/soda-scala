import sbt._

object Dependencies {
  object versions {
    val asyncHttpClient = "1.7.5"
    val jodaConvert = "1.2"
    val jodaTime = "2.1"
    val rojomaJson = "1.4.7"
    val scalaCheck_28 = "1.8"
    val scalaCheck_29 = "1.10.0"
    val scalaTest = "1.8"
    val slf4j = "1.6.6"
  }

  val asyncHttpClient = "com.ning" % "async-http-client" % versions.asyncHttpClient

  val jodaConvert = "org.joda" % "joda-convert" % versions.jodaConvert

  val jodaTime = "joda-time" % "joda-time" % versions.jodaTime

  val rojomaJson = "com.rojoma" %% "rojoma-json" % versions.rojomaJson

  def scalaCheck(implicit scalaVersion: String) = ScalaVersion.v match {
    case Scala28 => "org.scalacheck" % "scalacheck_2.8.1" % versions.scalaCheck_28
    case Scala29 => "org.scalacheck" %% "scalacheck" % versions.scalaCheck_29
  }

  val scalaTest = "org.scalatest" %% "scalatest" % versions.scalaTest

  val slf4j = "org.slf4j" % "slf4j-api" % versions.slf4j
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % versions.slf4j
}
