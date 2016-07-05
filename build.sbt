name          := "SlateEmergencyContactTransfer"
organization  := "edu.eckerd"
version       := "0.0.1"
scalaVersion  := "2.11.8"
scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

unmanagedBase := baseDirectory.value / ".lib"

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
    "com.typesafe.slick" %% "slick-hikaricp" % slickV
  )
}


//enablePlugins(JavaAppPackaging)
