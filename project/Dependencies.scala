import sbt._

object Dependencies {
  object versions {
    val asyncHttpClient = "1.7.5"
    val jodaConvert = "1.2"
    val jodaTime = "2.1"
    val rojomaJson_29 = "2.0.0"
    val rojomaJson_210 = "[2.1.0,3.0.0)"
    val scalaCheck = "1.10.0"
    val scalaTest = "1.9.1"
    val slf4j = "1.6.6"
  }

  val asyncHttpClient = "com.ning" % "async-http-client" % versions.asyncHttpClient

  val jodaConvert = "org.joda" % "joda-convert" % versions.jodaConvert

  val jodaTime = "joda-time" % "joda-time" % versions.jodaTime

  def rojomaJson(implicit sv: String) = ScalaVersion.v(sv) match {
    case Scala29 => "com.rojoma" %% "rojoma-json" % versions.rojomaJson_29
    case Scala210 => "com.rojoma" %% "rojoma-json" % versions.rojomaJson_210
  }

  val scalaCheck = "org.scalacheck" %% "scalacheck" % versions.scalaCheck

  val scalaTest = "org.scalatest" %% "scalatest" % versions.scalaTest

  val slf4j = "org.slf4j" % "slf4j-api" % versions.slf4j
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % versions.slf4j
}
