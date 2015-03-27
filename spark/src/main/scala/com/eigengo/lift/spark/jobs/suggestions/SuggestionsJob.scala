package com.eigengo.lift.spark.jobs.suggestions

import java.util.{UUID, Date}
import java.util.concurrent.TimeUnit

import akka.analytics.cassandra._
import com.eigengo.lift.Suggestion.Session
import com.eigengo.lift.SuggestionSource.Programme
import com.eigengo.lift.exercise.UserExercises.SessionStartedEvt
import com.eigengo.lift.spark.api.{ExerciseMarshallers, HttpClient}
import com.eigengo.lift.spark.jobs.Batch
import com.eigengo.lift.{MuscleGroups, Suggestion, Suggestions}
import com.typesafe.config.Config
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.HashingTF
import org.apache.spark.ml.param.{ParamPair, ParamMap}
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.DataType
import spray.client.pipelining._
import spray.http.Uri.Path
import org.apache.spark.mllib.rdd.RDDFunctions._
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Try, _}

object SuggestionsJob {
  val weightedMuscleGroups = MuscleGroups.supportedMuscleGroups.zip(0d to 1 by 1d / MuscleGroups.supportedMuscleGroups.size)
}

object VectorType extends HashingTF {
  val VectorUDT: DataType = outputDataType
}

/**
 * Spark job suggesting exercises based on various parameters
 * Reads history from cassandra, uses trainers hints and programmes
 *
 * Currently a naive bayes classifier used for muscle groups
 * and linear regression for intensities
 *
 * Training data are N previous sessions
 * Testing data for today+1 are N previous sessions
 * Testing data for today+2 are N-1 previous sessions and 1 prediction
 */
class SuggestionsJob() extends Batch[Unit, Unit] with HttpClient with ExerciseMarshallers {

  override def name: String = "Test exercise suggestions"

  override def execute(sc: SparkContext, config: Config, params: Unit): Future[Either[String, Unit]] = {
    implicit val sqlContext = new org.apache.spark.sql.SQLContext(sc)

    val result = Try {
      val sessionEndedBefore = config.getDuration("jobs.suggestions.includeUsersSessionEndedBefore", TimeUnit.MILLISECONDS).milliseconds.toMillis
      val historySize = config.getInt("jobs.suggestions.historySizeParameter")
      val futureSize = config.getInt("jobs.suggestions.futureSizeParameter")

      val events = sc.eventTable().cache()

      val intensityPredictor = new LinearRegression()
        .setLabelCol("intensityLabel")
        .setFeaturesCol("intensityFeatures")
        .setPredictionCol("intensityPredictions")
      val muscleGroupKeysPredictor = new LinearRegression()
        .setLabelCol("muscleGroupsKeysLabel")
        .setFeaturesCol("muscleGroupsKeysFeatures")
        .setPredictionCol("muscleGroupsKeysPredictions")
      val normalizer = new MuscleGroupsKeysNormalizer()
      val intensityFeatureExtractor = new IntensityFeatureExtractor()
      val muscleGroupKeysFeatureExtractor = new MuscleGroupKeysFeatureExtractor()
      val denormalizer = new MuscleGroupsKeysDenormalizer()
      val userFilter = new UserFilter()

      val dataFrame = SessionStartedEvtPreProcessor.preprocess(events)

      val muscleGroupKeysPipeline = new Pipeline().setStages(Array(
        userFilter,
        normalizer,
        muscleGroupKeysFeatureExtractor,
        muscleGroupKeysPredictor
      ))

      val intensityPipeline = new Pipeline().setStages(Array(
        userFilter,
        intensityFeatureExtractor,
        intensityPredictor
      ))

      EligibleUsers.getEligibleUsers(events, sessionEndedBefore)
        .collect()
        .map { u =>
          val muscleGroupKeysModel = muscleGroupKeysPipeline.fit(
            dataFrame,
            ParamMap(
              ParamPair(UserFilter.userIdParam, u),
              ParamPair(MuscleGroupKeysFeatureExtractor.useHistoryParam, 5)))

          val intensityModel = intensityPipeline.fit(
            dataFrame,
            ParamMap(
              ParamPair(UserFilter.userIdParam, u),
              ParamPair(IntensityFeatureExtractor.useHistoryParam, 5)))

          //TODO: Prepate test dataset
          //TODO: And use futureSizeParameter
          val testData = dataFrame

          val muscleGroupsKeysPredicted = denormalizer
            .transform(muscleGroupKeysModel.transform(testData))
            .select("muscleGroupsKeysDenormalized")
            .collect()

          val intensityPredicted = intensityModel.transform(testData)
            .select("intensityPredictions")
            .collect()

          submitResult(
            u,
            muscleGroupsKeysPredicted
              .zip(intensityPredicted)
              .zip(0 to muscleGroupsKeysPredicted.size - 1)
              .map(q => (q._1._1.getString(0), q._1._2.getDouble(0), TimeUtil.addDays(new Date(), q._2))).toSeq,
            config)
      }
    }

    //TODO: Refactor error handling
    result match {
      case Success(f) =>
        Future.sequence(f.toList).map { a =>
          val failed = a.filter(_.isLeft)
          if (failed.isEmpty) Right((): Unit) else Left(failed.mkString(","))
        }

      case Failure(e) => Future(Left(e.toString))
    }
  }

  private def submitResult(userId: String, suggestions: Seq[(String, Double, Date)], config: Config): Future[Either[String, String]] =
    request(
      uri => Post(uri.withPath(Path(s"/exercise/$userId/suggestions")), buildSuggestions(suggestions)),
      config)

  private def buildSuggestions(suggestions: Seq[(String, Double, Date)]): Suggestions =
    Suggestions(suggestions.map(s => buildSuggestion(s._3, s._1, s._2)).toList)

  private def buildSuggestion(date: Date, exercise: String, intensity: Double): Suggestion =
    Session(date, Programme, Seq(exercise), intensity)
}