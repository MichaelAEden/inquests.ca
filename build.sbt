import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enablePlugins(JavaAppPackaging, AshScriptPlugin)

name := "inquests.ca"

version := "0.1"

scalaVersion := "2.12.8"

dockerUpdateLatest := true
dockerBaseImage := "openjdk:8-jre-alpine"
dockerUsername := Some("michaelaeden")
packageName in Docker := "inquests-ca"

// Note that running 'sbt docker:clean' will fail because it will attempt to
// untag the image with the below tag
version in Docker := LocalDateTime.now.format(
  DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss")
)

val akkaVersion = "2.5.22"
val akkaHttpVersion = "10.1.8"
val circeVersion = "0.10.0"
val slickVersion = "3.3.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "de.heikoseeberger" %% "akka-http-circe" % "1.25.2",

  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,

  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "mysql" % "mysql-connector-java" % "8.0.16",
  "com.h2database" % "h2" % "1.3.148",
  "org.slf4j" % "slf4j-nop" % "1.6.4"
)
