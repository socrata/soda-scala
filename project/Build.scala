import sbt._
import Keys._

object Build extends sbt.Build {
  lazy val build = Project(
    "soda-scala",
    file("."),
    settings = BuildSettings.buildSettings ++ BuildSettings.sonatypeSettings
  ) aggregate (allOtherProjects: _*) dependsOn(sodaConsumer, sodaPublisher)

  private def allOtherProjects =
    for {
      method <- getClass.getDeclaredMethods.toSeq
      if method.getParameterTypes.isEmpty && classOf[Project].isAssignableFrom(method.getReturnType) && method.getName != "build"
    } yield method.invoke(this).asInstanceOf[Project] : ProjectReference

  private def p(name: String, settings: { def settings: Seq[Setting[_]] }, dependencies: ClasspathDep[ProjectReference]*) =
    Project(name, file(name), settings = settings.settings) dependsOn(dependencies: _*)

  lazy val sodaConsumer = p("soda-consumer-scala", SodaConsumer)

  lazy val sodaPublisher = p("soda-publisher-scala", SodaPublisher, sodaConsumer)

  lazy val sodaSample = p("soda-scala-sample", SodaSample, sodaPublisher)
}
