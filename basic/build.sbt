import Dependencies._
import Keys._

Build.Settings.project

name := "basic"

libraryDependencies ++= Seq(
  akka.analytics_cassandra % "provided",
  spark.core % "provided",
  hadoop.client % "provided",

  akka.analytics_cassandra % "runtime",
  akka.kryo % "runtime",
  guava % "runtime"
)
