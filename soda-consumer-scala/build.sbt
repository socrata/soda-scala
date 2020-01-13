import Dependencies._

name := "soda-consumer-scala"

libraryDependencies := Seq(
  asyncHttpClient,
  jodaConvert,
  jodaTime,
  rojomaJson,
  scalaTest % "test",
  scalaCheck % "test"
)
