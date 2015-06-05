package io.muvr.analytics.basic

import java.util.{Calendar, Date}

import akka.analytics.cassandra
import akka.analytics.cassandra.JournalKey
import io.muvr.UserId
import io.muvr.exercise.{EntireResistanceExerciseSession, UserExerciseProcessorPersistenceId}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object IntensityMain {
  import CommonPipeline._
  import IntensityPipeline._
  import SparkConfiguration._

  def main(args: Array[String]) {
    import cassandra._

    val sc = new SparkContext(sparkConf)
    val et = sc.eventTable().cache()
    val preparedIntensityPipeline = pipeline(et, 50, 10)
    getUsers(et)
      .collect()
      .map(userId ⇒ userId → preparedIntensityPipeline(userId))
      .foreach(println)
    println("done")
  }

}

object CommonPipeline {
  type RawInputData = RDD[(JournalKey, Any)]
  type FilteredInputData = RDD[(UserId, Any)]

  implicit class RichDate(date: Date) {

    def addDays(count: Int): Date = {
      val c = Calendar.getInstance()
      c.setTime(date)
      c.add(Calendar.DAY_OF_YEAR, count)
      c.getTime
    }

    val midnight: Date = {
      val c = Calendar.getInstance()
      c.setTime(date)
      c.set(Calendar.MILLISECOND, 0)
      c.set(Calendar.SECOND, 0)
      c.set(Calendar.MINUTE, 0)
      c.set(Calendar.HOUR_OF_DAY, 0)
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
  import org.apache.spark.mllib.linalg.Vectors
  import org.apache.spark.mllib.rdd.RDDFunctions._
  import org.apache.spark.mllib.regression.{LabeledPoint, LinearRegressionWithSGD}

  type ERESFilteredInputData = RDD[EntireResistanceExerciseSession]
  type PredictionPipeline = UserId ⇒ PredictorResult
  type PredictorResult = Seq[(String, Double, Date)]

  def filterEvents(userId: UserId, events: RawInputData): ERESFilteredInputData = {
    events.flatMap {
      case (JournalKey(UserExerciseProcessorPersistenceId(`userId`), _, _), eres: EntireResistanceExerciseSession) ⇒ Some(eres)
      case _ => None
    }.distinct()
  }

  def pipeline(events: RawInputData, useHistory: Int, predictDays: Int): PredictionPipeline = { userId ⇒
    val allInputData = filterEvents(userId, events)
    val distinctMuscleGroupIds = allInputData.flatMap(_.session.muscleGroupIds).distinct().collect()

    distinctMuscleGroupIds.flatMap { mgid ⇒
      val inputData = allInputData.filter(_.session.muscleGroupIds.contains(mgid))
      val inputDataSize = inputData.count()

      if (inputDataSize == 0) Nil
      else {
        val normalizedUseHistory = Math.min(useHistory, inputDataSize.toInt - 1)
        var intensityPredictions: List[(String, Double, Date)] = Nil

        val now = new Date().midnight

        val intensityTrainingData = inputData
          .map(_.session.intendedIntensity)
          .sliding(normalizedUseHistory + 1)
          .map(exercises => LabeledPoint(exercises.head, Vectors.dense(exercises.tail)))

        val intensityModel = LinearRegressionWithSGD.train(intensityTrainingData, 100, 0.01, 1.0)
        val indexedInputData = inputData.zipWithIndex().cache()
        for (i ← 0 to predictDays - 1) {
          val historyTestData = indexedInputData.filter(x => x._2 > inputDataSize - normalizedUseHistory - 1 + i)
          val padWithPredictions = normalizedUseHistory - historyTestData.count().toInt
          val paddedTestData: Array[Double] = if (padWithPredictions > 0) {
            historyTestData.map(_._1.session.intendedIntensity).collect() ++
              intensityPredictions.take(Math.min(intensityPredictions.size, padWithPredictions)).map(_._2) ++
              Array.fill(Math.min(0, padWithPredictions - intensityPredictions.size))(0.5)
          } else historyTestData.map(_._1.session.intendedIntensity).collect()

          require(paddedTestData.length == normalizedUseHistory)

          val predictedIntensity = intensityModel.predict(Vectors.dense(paddedTestData))

          intensityPredictions = intensityPredictions :+ (mgid, predictedIntensity, now.addDays(i + 1))
        }

        intensityPredictions
      }
    }

  }

}




//    val muscleGroupsInputData = allInputData
//      .map(_.session.muscleGroupIds)
//      .distinct()
//
//    val muscleGroupsInputDataSize = muscleGroupsInputData.count()
//
//    val normalizedUseHistory = Math.min(useHistory, muscleGroupsInputDataSize.toInt - 1)
//    val muscleGroupsTrainingData = muscleGroupsInputData
//      .zipWithIndex()
//      .map(_._2.toDouble)
//      .sliding(normalizedUseHistory + 1)
//      .map(mgs => LabeledPoint(mgs.head, Vectors.dense(mgs.tail)))
//
//    val muscleGroupsModel = NaiveBayes.train(muscleGroupsTrainingData)
//    val indexedInputData = muscleGroupsInputData.zipWithIndex().map(_._2.toDouble)
//    var predictions: List[(Double, Date)] = Nil
//    for (i <- 0 to predictDays - 1) {
//      val historyTestData = indexedInputData
//        .filter(x => x._2 > muscleGroupsInputDataSize - normalizedUseHistory - 1 + i)
//
//      val padWithPredictions = normalizedUseHistory - historyTestData.count().toInt
//
//      val paddedTestData = if (padWithPredictions > 0) {
//        historyTestData.map(_._1).collect() ++
//          predictions.take(Math.min(predictions.size, padWithPredictions)).map(x => (x._1, x._2)) ++
//          Array.fill(Math.min(0, padWithPredictions - predictions.size))((0.5, 0.5))
//      } else {
//        historyTestData.map(_._1).collect()
//      }
//
//      require(paddedTestData.size == normalizedUseHistory)
//
//      val predictedMuscleKeyGroup = muscleGroupsModel.predict(Vectors.dense(paddedTestData.map(_._1)))
//      val predictedIntensity = intensityModel.predict(Vectors.dense(paddedTestData.map(_._2)))
//
//      predictions = predictions.::((predictedMuscleKeyGroup, predictedIntensity, addDays(now, i + 1)))
//    }
//
