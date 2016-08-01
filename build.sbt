import de.heikoseeberger.sbtheader.license.Apache2_0

name          := "SlateEmergencyContactTransfer"
organization  := "edu.eckerd"
maintainer := "Christopher Davenport <ChristopherDavenport@outlook.com>"
packageSummary := "A transfer application that moves Emergency Contact Information Between Slate and Banner"


version       := "0.1.0"
scalaVersion  := "2.11.8"
scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

unmanagedBase := baseDirectory.value / ".lib"

mainClass in Compile := Some("edu.eckerd.integrations.slate.emergencycontact.MainApplication")

libraryDependencies ++= {
  val akkaV             = "2.4.7"
  val scalaTestV        = "2.2.6"
  val slickV            = "3.1.1"
  Seq(
    "com.typesafe.akka" %% "akka-http-core"                    % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental"            % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
    "org.scalatest"     %% "scalatest"                         % scalaTestV       % "test",
    "com.typesafe.akka" %% "akka-http-testkit"                 % akkaV            % "test",
    "com.typesafe" % "config" % "1.3.0",
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
    "com.typesafe.slick" %% "slick" % slickV,
    "com.typesafe.slick" %% "slick-extensions" % "3.1.0" ,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "me.lessis" %% "courier" % "0.1.3",
    "com.googlecode.libphonenumber" % "libphonenumber" % "7.5.1"
  )
}

headers := Map(
  "scala" -> Apache2_0("2016", "Eckerd College"),
  "conf" -> Apache2_0("2016", "Eckerd College", "#")
)

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/maven-releases/"

mappings in Universal ++= Seq(
  sourceDirectory.value / "main" / "resources" / "application.conf" -> "conf/application.conf",
  sourceDirectory.value / "main" / "resources" / "logback.xml" -> "conf/logback.xml"
)

rpmVendor := "Eckerd College"
rpmLicense := Some("Apache 2.0")

enablePlugins(JavaAppPackaging)
