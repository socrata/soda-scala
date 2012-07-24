import sbt._
import Keys._

import Dependencies._

object SocrataConsumer {
  lazy val settings: Seq[Setting[_]] = BuildSettings.commonProjectSettings() ++ Seq(
    libraryDependencies <++= scalaVersion(libraries(_))
  )

  def libraries(implicit scalaVersion: String) = Seq(
    asyncHttpClient,
    rojomaJson
  )
}
