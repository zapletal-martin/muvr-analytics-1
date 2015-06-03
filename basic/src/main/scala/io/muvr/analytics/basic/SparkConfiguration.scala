package io.muvr.analytics.basic

import org.apache.spark.SparkConf

object SparkConfiguration {

  lazy val sparkConf: SparkConf = {
    new SparkConf()
      .setAppName("Muvr Analytics")
      .set("spark.cassandra.connection.host", "127.0.0.1")
      .set("spark.cassandra.query.retry.count", "0")
      .set("spark.cassandra.connection.timeout_ms", "1000")
      .set("spark.cassandra.connection.reconnection_delay_ms.max", "5s")
      .set("spark.cassandra.journal.keyspace", "akka")
      .set("spark.cassandra.journal.table", "messages")
  }

}
