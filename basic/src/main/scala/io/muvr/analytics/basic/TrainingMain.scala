package io.muvr.analytics.basic

import akka.analytics.cassandra.JournalKey
import io.muvr.exercise.{EntireResistanceExerciseSession, UserExerciseProcessorPersistenceId}
import org.apache.spark.SparkContext

/**
 * Reads the submitted examples
 */
object TrainingMain {
  import SparkConfiguration._

  def main(args: Array[String]) {
    import cassandrax._

    val sc = new SparkContext(sparkConf)
    val allExamples = sc.eventTable()
      .flatMap { case (JournalKey(UserExerciseProcessorPersistenceId(userId), _, _), EntireResistanceExerciseSession(_, _, examples)) ⇒ examples }
      .flatMap { example ⇒ example.correct.map(correct ⇒ correct → example.fusedSensorData) }
      .groupBy { _._1 }

    allExamples.foreach(println)
  }
}
