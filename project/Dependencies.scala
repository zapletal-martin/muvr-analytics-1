import sbt._

object Dependencies {

  object spray {
    val version = "1.3.3"

    val client  = "io.spray" %% "spray-client"             % version
  }

  //TODO: Spark, Hadoop, Akkaanalytics are exclusive for currently used spark build and should be separated from the rest
  object hadoop {
    val version = "2.4.0"

    val client = ("org.apache.hadoop" % "hadoop-client" % version)
      /*.exclude("commons-beanutils", "commons-beanutils")
      .exclude("commons-beanutils", "commons-beanutils-core")
      .exclude("commons-logging", "commons-logging")
      .exclude("org.slf4j", "slf4j-simple")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("com.google.guava", "guava")*/
  }

  object spark {
    val version = "1.3.1"

    val core = ("org.apache.spark" %% "spark-core" % version)
      .exclude("org.apache.hadoop", "hadoop-client")
      .exclude("org.spark-project.akka", "akka-remote_2.10")
      .exclude("org.apache.hadoop", "hadoop-common")
      .exclude("org.apache.hadoop", "hadoop-hdfs")
      .exclude("org.apache.curator", "curator-framework")
      .exclude("org.apache.curator", "curator-recipes")
      .exclude("org.scala-lang", "scalap")
      .exclude("com.google.code.findbugs", "jsr305")
      /*.exclude("org.slf4j", "slf4j-log4j12")
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
      .exclude("com.google.guava", "guava")*/
    //val mllib = ("org.apache.spark" %% "spark-mllib" % version)
      /*.exclude("org.slf4j", "slf4j-api")*/
    //val streaming = "org.apache.spark" %% "spark-streaming" % version
    /*val streamingKafka = ("org.apache.spark" %% "spark-streaming-kafka" % version)
      .exclude("commons-beanutils", "commons-beanutils")
      .exclude("commons-beanutils", "commons-beanutils-core")
      .exclude("commons-collections", "commons-collections")
      .exclude("com.esotericsoftware.minlog", "minlog")*/
  }

  object akka {
    val version = "0.2"

    val analytics_cassandra = ("com.github.krasserm" %% "akka-analytics-cassandra" % version)
      .exclude("com.typesafe.akka", "akka-actor_2.10")
      .exclude("com.esotericsoftware.minlog", "minlog")
      .exclude("commons-beanutils", "commons-beanutils-core")
      .exclude("commons-collections", "commons-collections")
      .exclude("org.slf4j", "jcl-over-slf4j")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("org.apache.spark", "spark-core_2.10")
      .exclude("commons-logging", "commons-logging")
      .exclude("com.codahale.metrics", "metrics-core")
      .exclude("com.google.guava", "guava")
  }

  // Testing
  val scalatest        = "org.scalatest"          %% "scalatest"    % "2.2.4"
  val scalacheck       = "org.scalacheck"         %% "scalacheck"   % "1.12.2"
}
