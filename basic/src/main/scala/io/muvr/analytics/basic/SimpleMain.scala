package io.muvr.analytics.basic

import akka.analytics.cassandra
import org.apache.spark.SparkContext

object SimpleMain {
  import SparkConfiguration._

  def main(args: Array[String]) {
    import cassandra._

    val sc = new SparkContext(sparkConf)
    sc.eventTable().cache().foreach(println)
  }
}
