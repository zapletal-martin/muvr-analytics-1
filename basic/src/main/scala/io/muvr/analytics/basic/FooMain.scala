package io.muvr.analytics.basic

import akka.analytics.cassandra
import org.apache.spark.{SparkConf, SparkContext}

object FooMain {

  private def sparkConf(): SparkConf = {
    val conf = new SparkConf().setAppName("Simple Application")
    conf
      .set("spark.cassandra.connection.host", "127.0.0.1")
      .set("spark.cassandra.query.retry.count", "0")
      .set("spark.cassandra.connection.timeout_ms", "1000")
      .set("spark.cassandra.connection.reconnection_delay_ms.max", "5s")
      .set("spark.cassandra.journal.keyspace", "akka")
      .set("spark.cassandra.journal.table", "messages")
  }

  def main(args: Array[String]) {
    import cassandra._

    val sc = new SparkContext(sparkConf())
    sc.eventTable().cache().foreach(println)
  }
}
