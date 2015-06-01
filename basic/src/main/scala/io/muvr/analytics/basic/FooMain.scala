package io.muvr.analytics.basic

import akka.actor.ActorSystem
import akka.analytics.cassandra.JournalKey
import akka.persistence.PersistentRepr
import akka.serialization.SerializationExtension
import io.muvr.exercise.Rest
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.{Failure, Success}

object cassandra {
  import com.datastax.spark.connector._
  import com.datastax.spark.connector.types._
  private case object Ignore

  implicit object JournalEntryTypeConverter extends TypeConverter[PersistentRepr] {
    import scala.reflect.runtime.universe._

    val converter = implicitly[TypeConverter[Array[Byte]]]
//    def kryoInstantiator: KryoInstantiator =
//      (new ScalaKryoInstantiator)
//    def poolSize: Int = {
//      val GUESS_THREADS_PER_CORE = 4
//      GUESS_THREADS_PER_CORE * Runtime.getRuntime.availableProcessors
//    }
//    @transient val kryoPool: KryoPool =
//      KryoPool.withByteArrayOutputStream(poolSize, kryoInstantiator)
//

    // FIXME: how to properly obtain an ActorSystem in Spark tasks?
    def cl(): ClassLoader = Rest.getClass.getClassLoader()
    println(cl().loadClass("io.muvr.exercise.Rest"))

    @transient lazy val system = ActorSystem("TypeConverter", None, Some(cl()))
    @transient lazy val serial = SerializationExtension(system)

    def targetTypeTag = implicitly[TypeTag[PersistentRepr]]
    def convertPF = {
      case obj => deserialize(converter.convert(obj))
    }

    def deserialize(bytes: Array[Byte]): PersistentRepr =
//      scala.util.Try(kryoPool.fromBytes(bytes)) match {
//        case scala.util.Success(p: PersistentRepr) ⇒ p.update(sender = null)
//        case scala.util.Success(x) ⇒
//          println(s"Success($x)")
//          PersistentRepr(Ignore)
//        case scala.util.Failure(x) ⇒
//          println(s"Failure($x)")
//          PersistentRepr(Ignore) // headers, confirmations, etc ...
//      }
      serial.deserialize(bytes, classOf[PersistentRepr]) match {
        case Success(p) ⇒ p.update(sender = null)
        case Failure(x) ⇒
          println(x)
          PersistentRepr(Ignore) // headers, confirmations, etc ...
    }
  }

  TypeConverter.registerConverter(JournalEntryTypeConverter)

  implicit class JournalSparkContext(context: SparkContext) {
    val keyspace = context.getConf.get("spark.cassandra.journal.keyspace", "akka")
    val table = context.getConf.get("spark.cassandra.journal.table", "messages")

    val journalKeyEventPair = (persistenceId: String, partition: Long, sequenceNr: Long, message: PersistentRepr) =>
      (JournalKey(persistenceId, partition, sequenceNr), message.payload)

    def eventTable(): RDD[(JournalKey, Any)] =
      context.cassandraTable(keyspace, table).select("processor_id", "partition_nr", "sequence_nr", "message").as(journalKeyEventPair).filter(_._2 != Ignore)
  }

}

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
      //.set("spark.executor.userClassPathFirst", "true")
  }

  def main(args: Array[String]) {
    import cassandra._

    val sc = new SparkContext(sparkConf())
    println("1")
    sc.eventTable().cache().foreach(println)
    println("2")
  }
}
