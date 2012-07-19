import sbt._

object Build extends sbt.Build {
  lazy val build = Project(
    "socrata-api",
    file("."),
    settings = BuildSettings.buildSettings
  ) aggregate (allOtherProjects: _*)

  private def allOtherProjects =
    for {
      method <- getClass.getDeclaredMethods.toSeq
      if method.getParameterTypes.isEmpty && classOf[Project].isAssignableFrom(method.getReturnType) && method.getName != "build"
    } yield method.invoke(this).asInstanceOf[Project] : ProjectReference

  private def p(name: String, settings: { def settings: Seq[Setting[_]] }, dependencies: ClasspathDep[ProjectReference]*) =
    Project(name, file(name), settings = settings.settings) dependsOn(dependencies: _*)

  lazy val socrataConsumer = p("socrata-consumer-scala", SocrataConsumer)

  lazy val socrataPublisher = p("socrata-publisher-scala", SocrataPublisher, socrataConsumer)

  lazy val socrataSample = p("socrata-scala-sample", SocrataSample, socrataPublisher)
}
