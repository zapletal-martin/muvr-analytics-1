package io.muvr.analytics.basic

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
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryo.classesToRegister", "")
      .set("spark.cassandra.journal.table", "messages")
  }

  def main(args: Array[String]) {
    import akka.analytics.cassandra._
    import com.datastax.spark.connector._

    val sc = new SparkContext(sparkConf())
    println("1")
    //val events = sc.cassandraTable("akka", "messages").select("processor_id", "partition_nr", "sequence_nr", "message")
    //events.foreach(println)
    val events = sc.eventTable().cache()
      .foreach(println)
  }
}
