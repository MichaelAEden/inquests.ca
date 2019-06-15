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
val akkaHttpCirceVersion = "1.25.2"
val scalaTestVersion = "3.0.5"
val slickVersion = "3.3.1"
val mySqlConnectorVersion = "8.0.16"

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
  "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion,

  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,

  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "mysql" % "mysql-connector-java" % mySqlConnectorVersion
)
