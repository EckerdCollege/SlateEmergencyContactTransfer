//name          := "SlateEmergencyContactTransfer"
organization  := "edu.eckerd"
//maintainer := "Christopher Davenport <ChristopherDavenport@outlook.com>"
//packageSummary := "A transfer application that moves Emergency Contact Information Between Slate and Banner"


version       := "0.1.1"
scalaVersion  := "2.12.2"
crossScalaVersions := Seq("2.11.11", scalaVersion.value)

scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

unmanagedBase := baseDirectory.value / ".lib"

mainClass in Compile := Some("edu.eckerd.integrations.slate.emergencycontact.MainApplication")

libraryDependencies ++= {
  val scalaTestV        = "3.0.1"
  val slickV            = "3.2.0"
  Seq(
    "edu.eckerd" %% "slate-core" % "0.1.1",
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "com.typesafe.slick" %% "slick" % slickV,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "org.apache.commons" % "commons-email" % "1.4",
    "com.googlecode.libphonenumber" % "libphonenumber" % "7.5.1"
  )
}

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/maven-releases/"
)


