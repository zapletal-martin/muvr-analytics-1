import sbt._
import Keys._

object Dependencies {

  object spray {
    val version = "1.3.2"

    val client  = "io.spray" %% "spray-client"             % version
  }

  //TODO: Spark, Hadoop, Akkaanalytics are exclusive for currently used spark build and should be separated from the rest
  object hadoop {
    val version = "2.4.0"

    val client = ("org.apache.hadoop" % "hadoop-client" % version)
      .exclude("commons-beanutils", "commons-beanutils")
      .exclude("commons-beanutils", "commons-beanutils-core")
      .exclude("commons-logging", "commons-logging")
      .exclude("org.slf4j", "slf4j-simple")
      .exclude("org.slf4j", "slf4j-log4j12")
  }

  object spark {
    val version = "1.2.0"

    val core = ("org.apache.spark" %% "spark-core" % version)
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.eclipse.jetty.orbit", "javax.transaction")
      .exclude("org.eclipse.jetty.orbit", "javax.mail")
      .exclude("org.eclipse.jetty.orbit", "javax.mail.glassfish")
      .exclude("org.eclipse.jetty.orbit", "javax.activation")
      .exclude("commons-beanutils", "commons-beanutils")
      .exclude("commons-beanutils", "commons-beanutils-core")
      .exclude("commons-collections", "commons-collections")
      .exclude("commons-logging", "commons-logging")
      .exclude("com.esotericsoftware.minlog", "minlog")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("org.apache.hadoop", "hadoop-yarn-api")
    val mllib = ("org.apache.spark" %% "spark-mllib" % version)
      .exclude("org.slf4j", "slf4j-api")
    val streaming = "org.apache.spark" %% "spark-streaming" % version
    val streamingKafka = ("org.apache.spark" %% "spark-streaming-kafka" % version)
      .exclude("commons-beanutils", "commons-beanutils")
      .exclude("commons-beanutils", "commons-beanutils-core")
      .exclude("commons-collections", "commons-collections")
      .exclude("com.esotericsoftware.minlog", "minlog")
  }

  object akkaAnalytics {
    val version = "0.2"

    val cassandra = ("com.github.krasserm" % "akka-analytics-cassandra_2.10" % version)
      .exclude("com.typesafe.akka", "akka-actor_2.10")
      .exclude("com.esotericsoftware.minlog", "minlog")
      .exclude("commons-beanutils", "commons-beanutils-core")
      .exclude("commons-collections", "commons-collections")
      .exclude("org.slf4j", "jcl-over-slf4j")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("org.apache.spark", "spark-core_2.10")
      .exclude("commons-logging", "commons-logging")
  }

  object slf4j {
    val version = "1.6.1"

    val slf4j_simple     = "org.slf4j"              % "slf4j-simple" % version
    val slf4j_api        = "org.slf4j"              % "slf4j-api"    % version
  }

  val scodec_bits      = "org.typelevel"          %% "scodec-bits"  % "1.0.4"

  // Testing
  val scalatest        = "org.scalatest"          %% "scalatest"    % "2.2.1"
  val scalacheck       = "org.scalacheck"         %% "scalacheck"   % "1.12.1"

}
