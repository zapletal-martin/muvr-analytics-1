package io.muvr.analytics.basic

import java.util.{Calendar, Date}

import akka.analytics.cassandra
import akka.analytics.cassandra.JournalKey
import breeze.linalg.normalize
import io.muvr.UserId
import io.muvr.exercise.{EntireResistanceExerciseSession, UserExerciseProcessorPersistenceId}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object IntensityMain {
  import SparkConfiguration._
  import IntensityPipeline._
  import CommonPipeline._

  def main(args: Array[String]) {
    import cassandra._

    val sc = new SparkContext(sparkConf)
    val et = sc.eventTable().cache()
    val preparedIntensityPipeline = pipeline(et, 50, 10)
    getUsers(et).collect().map(preparedIntensityPipeline).foreach(println)
    println("done")
  }

}

object CommonPipeline {
  type RawInputData = RDD[(JournalKey, Any)]
  type FilteredInputData = RDD[(UserId, Any)]

  implicit class RichDate(date: Date) {

    def addDays(count: Int): Date = {
      val c = Calendar.getInstance()
      c.add(Calendar.DAY_OF_YEAR, count)
      c.getTime
    }
  }

  def getUsers(input: RawInputData): RDD[UserId] = input.flatMap {
    case (JournalKey(UserExerciseProcessorPersistenceId(userId), _, _), x) ⇒ Some(userId)
    case _ ⇒ None
  }.distinct()

}

object IntensityPipeline {
  import CommonPipeline._
  import org.apache.spark.mllib.rdd.RDDFunctions._
  import org.apache.spark.mllib.linalg.Vectors
  import org.apache.spark.mllib.regression.LabeledPoint
  import org.apache.spark.mllib.regression.LinearRegressionWithSGD

  type ERESFilteredInputData = RDD[EntireResistanceExerciseSession]
  type PredictionPipeline = UserId ⇒ PredictorResult
  type PredictorResult = Seq[(Double, Date)]

  def filterEvents(userId: UserId, events: RawInputData): ERESFilteredInputData = {
    events.flatMap {
      case (JournalKey(UserExerciseProcessorPersistenceId(`userId`), _, _), eres: EntireResistanceExerciseSession) ⇒ Some(eres)
      case _ => None
    }.distinct()
  }

  def pipeline(events: RawInputData, useHistory: Int, predictDays: Int): PredictionPipeline = { userId ⇒
    val inputData = filterEvents(userId, events)
    val inputDataSize = inputData.count()
    val normalizedUseHistory = Math.min(useHistory, inputDataSize.toInt - 1)

    println("****")

    val now = new Date()

    // RDD[LabeledPoint]
    val intensityTrainingData = inputData
      .map(_.session.intendedIntensity)
      .sliding(normalizedUseHistory + 1)
      .map(exercises => LabeledPoint(exercises.head, Vectors.dense(exercises.tail)))

    intensityTrainingData.foreach(println)

    val intensityModel = LinearRegressionWithSGD.train(intensityTrainingData, 100)
    var intensityPredictions: List[(Double, Date)] = Nil
    val indexedInputData = inputData.zipWithIndex().cache()
    for (i ← 0 to predictDays - 1) {
      val historyTestData = indexedInputData.filter(x => x._2 > inputDataSize - normalizedUseHistory - 1 + i)
      val padWithPredictions = normalizedUseHistory - historyTestData.count().toInt
      val paddedTestData: Array[Double] = if (padWithPredictions > 0) {
        historyTestData.map(_._1.session.intendedIntensity).collect() ++
          intensityPredictions.take(Math.min(intensityPredictions.size, padWithPredictions)).map(_._1) ++
          Array.fill(Math.min(0, padWithPredictions - intensityPredictions.size))(0.5)
      } else historyTestData.map(_._1.session.intendedIntensity).collect()

      require(paddedTestData.length == normalizedUseHistory)
      println(java.util.Arrays.toString(paddedTestData))

      val predictedIntensity = intensityModel.predict(Vectors.dense(paddedTestData))

      intensityPredictions = (predictedIntensity → now.addDays(i + 1)) :: intensityPredictions
    }

    println(intensityPredictions)

    intensityPredictions
  }

}
