import sbt._
import Keys._

import Dependencies._

object SocrataConsumer {
  lazy val settings: Seq[Setting[_]] = BuildSettings.commonProjectSettings() ++ Seq(
    libraryDependencies <++= scalaVersion(libraries(_))
  )

  def libraries(implicit scalaVersion: String) = Seq(
    asyncHttpClient,
    // jodaConvert is only necessary at compile-time, but also
    // necessary for compiling any dependencies because of a
    // longstanding bug (or rather, lack of following javac's
    // conventions) in the Scala compiler which causes it to not
    // ignore annotations of types which aren't on the classpath.
    jodaConvert,
    jodaTime,
    rojomaJson,

    scalaCheck % "test"
  )
}
