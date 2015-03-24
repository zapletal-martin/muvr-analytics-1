import sbt._
import Keys._

name := "spark"

// Spark
lazy val spark = project.in(file("spark"))

// The main aggregate
lazy val root = (project in file(".")).aggregate(spark)

fork in Test := false

fork in IntegrationTest := false

parallelExecution in Test := false

publishLocal := {}

publish := {}
