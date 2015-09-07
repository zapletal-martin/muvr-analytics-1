package io.muvr.analytics.basic

import org.apache.spark.SparkConf

object SparkConfiguration {

  // TODO: Use environment variables
  lazy val sparkConf: SparkConf = {
    new SparkConf()
      .setAppName("Muvr Analytics")
      .set("spark.cassandra.connection.host", "192.168.99.101")
      .set("spark.cassandra.query.retry.count", "0")
      .set("spark.cassandra.connection.timeout_ms", "1000")
      .set("spark.cassandra.connection.reconnection_delay_ms.max", "5000")
  }

}
