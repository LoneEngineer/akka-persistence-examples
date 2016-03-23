import sbt.Keys._

name := """akka-fsm-samples"""

fork in Test := true

scalaVersion := "2.11.7"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:reflectiveCalls",
  "-language:postfixOps",
  "-language:implicitConversions"
)

resolvers ++= Seq(
  Resolver.typesafeRepo("releases")
)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.11.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.16",
  "ch.qos.logback" % "logback-classic" % "1.1.5",
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
  "org.scalatest" %% "scalatest" % "2.2.5" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.4.1" % Test
)
