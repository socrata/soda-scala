resolvers ++= Seq(
  "socrata releases" at "http://repo.socrata.com/artifactory/libs-release",
  "DiversIT repo" at "http://repository-diversit.forge.cloudbees.com/release"
)

//addSbtPlugin("com.socrata" % "socrata-cloudbees-sbt" % "1.0.0")

// resolvers += Resolver.url("artifactory", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

// addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")
