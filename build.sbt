name := "dependablecharger"

version := "0.1"

scalaVersion := "2.12.8"

val akkaVersion = "2.5.21"
val akkaHttpVersion = "10.1.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.play" %% "play-json" % "2.7.2",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.25.2",
  "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "1.0-RC1",
  "org.scalatest" %% "scalatest" % "3.0.6" % Test,
  "org.mockito" % "mockito-core" % "2.25.0" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion
)
