package io.muvr.analytics.basic

import java.util.Date

import akka.analytics.cassandra
import akka.analytics.cassandra.JournalKey
import io.muvr.UserId
import io.muvr.exercise.UserExerciseProcessorPersistenceId
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object IntensityMain extends IntensityPipeline {
  import SparkConfiguration._

  def main(args: Array[String]) {
    import cassandra._

    val sc = new SparkContext(sparkConf)

    getEligibleUsers(sc.eventTable().cache()).foreach(println)
  }

}

trait IntensityPipeline {
  type PredictionPipeline = UserId ⇒ (UserId, DenormalizedPredictorResult)
  type RawInputData = RDD[(JournalKey, Any)]
  type NormalizedInputData = RDD[(Double, Double)]
  type PredictorResult = Seq[(Double, Double, Date)]
  type DenormalizedPredictorResult = Seq[(UserId, Double, Date)]

  def getEligibleUsers(events: RawInputData): RDD[UserId] = {
    events.flatMap {
      case (JournalKey(UserExerciseProcessorPersistenceId(k), _, _), e) ⇒ Some(k)
      case _ => None
    }.distinct()
  }

  def preprocess(events: RawInputData): PredictionPipeline = { userId ⇒
    userId → Seq()
  }

}
