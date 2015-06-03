import sbt._
import Keys._

name := "muvr-analytics"

// Common protocol
lazy val commonProtocol = project.in(file("common-protocol"))

// Exercise protocol
lazy val exerciseProtocol = project.in(file("exercise-protocol")).dependsOn(commonProtocol)

// Spark
lazy val basic = project.in(file("basic")).dependsOn(exerciseProtocol)

// The main aggregate
lazy val root = (project in file(".")).aggregate(basic)

fork in Test := false

fork in IntegrationTest := false

parallelExecution in Test := false

publishLocal := {}

publish := {}
