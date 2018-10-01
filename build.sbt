name := """eventstore-poc"""
organization := "com.chatroulette"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.6"

val akkaVersion = "2.5.17"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

val akkaDeps =
  Seq(
    "com.typesafe.akka" %% "akka-actor-typed",
    "com.typesafe.akka" %% "akka-persistence-typed"
  ).map(_ % akkaVersion)

libraryDependencies ++= akkaDeps

libraryDependencies += "com.geteventstore" %% "akka-persistence-eventstore" % "5.0.2"
libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.5.4"

libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.9.1"

//libraryDependencies ++= Seq(
//  "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
//  "ch.qos.logback"    %  "logback-classic" % "1.2.3"
//)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.chatroulette.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.chatroulette.binders._"