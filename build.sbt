name := "dependablecharger"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.7",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.20",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.play" %% "play-json" % "2.7.2",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.25.2",
  "org.scalatest" %% "scalatest" % "3.0.6" % Test,
  "org.mockito" % "mockito-core" % "2.25.0" % Test
)
