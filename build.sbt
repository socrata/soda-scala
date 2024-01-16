ThisBuild / organization := "com.socrata"

ThisBuild / version := "2.2.2"

ThisBuild / scalaVersion := "2.12.10"

ThisBuild / crossScalaVersions := Seq(scalaVersion.value, "2.10.4", "2.11.8")

// random stuff Sonatype wants
ThisBuild / pomExtra := (
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

lazy val sodaConsumer = project in file("soda-consumer-scala")

lazy val sodaPublisher = (project in file("soda-publisher-scala")).
  dependsOn(sodaConsumer)

lazy val sodaSample = (project in file("soda-scala-sample")).
  dependsOn(sodaPublisher)

publish / skip := true
