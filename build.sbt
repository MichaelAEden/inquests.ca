import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import sys.process._

enablePlugins(JavaAppPackaging, AshScriptPlugin)

name := "inquests.ca"

version := "0.1"

scalaVersion := "2.12.8"

dockerBaseImage := "openjdk:8-jre-alpine"
dockerUsername := Some("michaelaeden")
packageName in Docker := "inquests-ca"

// Note these methods assume image is built from within project directory.
// Run 'reload' in the sbt console to get the latest values.
def isMaster: Boolean = {
  val branchName = "git rev-parse --abbrev-ref HEAD".!!.trim
  branchName == "master"
}

def imageTag: String = {
  val branchName = "git rev-parse --abbrev-ref HEAD".!!.trim
  val commitHash = "git rev-parse HEAD".!!.trim.substring(0, 8)
  val date = LocalDateTime.now.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))

  s"${branchName}__${commitHash}__$date"
}

// Only update latest tag if current branch is master
dockerUpdateLatest := isMaster

// Note that running 'sbt docker:clean' may fail because it will attempt to
// untag an image with a tag that does not exist
version in Docker := imageTag

val akkaVersion = "2.5.22"
val akkaHttpVersion = "10.1.8"
val circeVersion = "0.10.0"
val slickVersion = "3.3.1"
val slf4jVersion = "1.7.28"

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

  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "slf4j-simple" % slf4jVersion,

  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",

  "com.google.firebase" % "firebase-admin" % "6.8.1"
)
