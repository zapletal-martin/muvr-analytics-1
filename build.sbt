import sbt._
import Keys._

name := "muvr-analytics"

// Exercise protocol
lazy val exerciseProtocol = project.in(file("exercise-protocol"))

// Spark
lazy val basic = project.in(file("basic")).dependsOn(exerciseProtocol)

// The main aggregate
lazy val root = (project in file(".")).aggregate(basic)

fork in Test := false

fork in IntegrationTest := false

parallelExecution in Test := false

publishLocal := {}

publish := {}
