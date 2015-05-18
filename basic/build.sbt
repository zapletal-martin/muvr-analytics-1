import Dependencies._
import Keys._

Build.Settings.project

name := "basic"

libraryDependencies ++= Seq(
  slf4j.slf4j_api,
  slf4j.slf4j_simple,
  akka.analytics_cassandra,
  spark.core,
  spark.mllib,
  spray.client
)
