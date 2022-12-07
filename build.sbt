name := "swatch"

organization := "com.mirkocaserta.swatch"

version := "1.0.1"

scalaVersion := "2.13.10"

val akkaVersion = "2.6.20"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.4.5" % "test",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "org.specs2" %% "specs2-core" % "4.19.2" % "test"
)

scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature")
