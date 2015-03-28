package com.eigengo.lift.spark.jobs.suggestions

import akka.analytics.cassandra.JournalKey
import com.eigengo.lift.exercise.UserExercises.SessionStartedEvt
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SQLContext}

/**
 * Preprocessing before DataFrame is created
 * E.g. to convert Cassandra events to just those we are interested in and transforming to DF
 *
 * @tparam T input RDD type
 */
trait RawDataPreProcessor[T] {

  /**
   * Preprocess RDD to DataFrame
   * @param rawData input RDD
   * @param sqlContext sqlContext needed to create DataFrame
   * @return DataFrame
   */
  def preprocess(rawData: RDD[T])(implicit sqlContext: SQLContext): DataFrame
}

/**
 * Creates DataFrame from input case class
 * This should work automatically using .toDF method on RDD, but its catalyst reflection won't recognize some types, e.g. UUID
 * I couldn't find a better way than to specify the schema manually in this case
 */
object SessionStartedEvtPreProcessor extends RawDataPreProcessor[(JournalKey, Any)] {
  override def preprocess(rawData: RDD[(JournalKey, Any)])(implicit sqlContext: SQLContext): DataFrame = {

    val rowRDD = rawData
      .flatMap {
        case (p, e) if e.isInstanceOf[SessionStartedEvt] => Some((p, e.asInstanceOf[SessionStartedEvt]))
        case _ => None
      }
      .map(toDFRow)

    sqlContext.createDataFrame(rowRDD, schema)
  }

  private def toDFRow(e: (JournalKey, SessionStartedEvt)): Row =
    Row(
      Row(
        e._1.persistenceId,
        e._1.partition,
        e._1.sequenceNr),
      Row(
        e._2.sessionId.toString
      ),
      Row(
        e._2.sessionProps.startDate,
        e._2.sessionProps.muscleGroupKeys.toArray.toBuffer,
        e._2.sessionProps.intendedIntensity,
        e._2.sessionProps.classification
      )
    )


  private lazy val schema = StructType(
    List(
      StructField("journalKey",
        StructType(
          List(
            StructField("persistenceId", StringType),
            StructField("partition", LongType),
            StructField("sequenceNr", LongType)
          )
        )
      ),
      StructField("sessionId", StringType),
      StructField("sessionProps",
        StructType(
          List(
            StructField("startDate", DateType),
            StructField("muscleGroupsKeys", ArrayType(StringType)),
            StructField("intendedIntensity", DoubleType),
            StructField("classification", StringType)
          )
        )
      )
    )
  )
}
