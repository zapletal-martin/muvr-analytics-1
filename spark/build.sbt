/**
 * Based on https://github.com/kevinschmidt/docker-spark
 *
 * Currently uses spark 1.2.0, Scala 2.11
 *
 * If made dependent on common results on large amount of new dependency conflicts including akka etc.
 *
 * Large amount of files causes "Invalid or Corrupt jarfile is encountered" due to bug in java 7
 * Possible workarounds include use of java 8 (current solution) or startup using java -cp instead of java -jar
 * See http://stackoverflow.com/questions/18441076/why-java-complains-about-jar-files-with-lots-of-entries
 *
 */

import Dependencies._
import Keys._

Build.Settings.project

name := "spark"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  slf4j.slf4j_api,
  slf4j.slf4j_simple,
  akkaAnalytics.cassandra,
  spark.core,
  spark.mllib,
  spray.client,
  scodec_bits
)

import DockerKeys._
import sbtdocker.ImageName
import sbtdocker.mutable.Dockerfile

dockerSettings

mainClass in assembly := Some("com.eigengo.lift.spark.Spark")

docker <<= (docker dependsOn Keys.`package`.in(Compile, packageBin))

dockerfile in docker := {
  val artifact = artifactPath.in(Compile, packageBin).value
  val artifactTargetPath = s"/app/${artifact.name}"
  val classpath = (managedClasspath in Compile).value
  val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))

  val classpathString = "/app/*" //classpath.files.map("/app/" + _.getName).mkString(":") + ":" + artifactTargetPath

  val libDirectory = file("./lib/")
  val libDirectoryPath = libDirectory.getAbsolutePath

  //TODO: All dependencies in one directory so they can be sent to Docker
  //Unfortunately docker has limit on number of commands so this needs to be done if we don't want to use assembly
  val createDir = s"mkdir $libDirectoryPath"
  createDir !

  val copyCmd = classpath.files.map(_.getAbsolutePath).mkString("cp -p ", " ", s" $libDirectoryPath")
  copyCmd !

  new Dockerfile {
    from("martinz/spark-singlenode:latest")

    add(artifact, artifactTargetPath)

    add(libDirectory, "/app/")
    //entryPoint("java", "-jar", artifactTargetPath)
    entryPoint("sh", "/root/spark_singlenode_files/default_cmd", classpathString, mainclass)
  }
}

imageName in docker := {
  ImageName(
    namespace = Some("janm399"),
    repository = "lift",
    tag = Some(name.value))
}
