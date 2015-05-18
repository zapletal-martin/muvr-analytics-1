import Dependencies._
import Keys._

Build.Settings.project

name := "basic"

libraryDependencies ++= Seq(
  akka.analytics_cassandra,
  spark.core,
  hadoop.client,
  spray.client
)
