name := "swatch"

organization := "com.mirkocaserta.swatch"

version := "1.0.1-SNAPSHOT"

scalaVersion := "2.12.6"

val akkaVersion = "2.5.11"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.11" % "test",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "3.1.1" % "test",
  "org.specs2" %% "specs2-core" % "4.8.3" % "test"
)

scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature")
