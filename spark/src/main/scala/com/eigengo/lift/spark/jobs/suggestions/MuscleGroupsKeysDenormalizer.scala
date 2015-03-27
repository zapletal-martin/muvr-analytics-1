package com.eigengo.lift.spark.jobs.suggestions

import java.util.Arrays._

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

class MuscleGroupsKeysDenormalizer extends Transformer {
  override def transform(dataset: DataFrame, paramMap: ParamMap): DataFrame =
    dataset
      .withColumn("muscleGroupsKeysDenormalized", callUDF((k: Double) => denormalize(k), StringType, dataset("muscleGroupsKeysPredictions")))

  @DeveloperApi
  override def transformSchema(schema: StructType, paramMap: ParamMap): StructType =
    StructType(schema.fields :+ StructField("muscleGroupsKeysDenormalized", StringType, true))

  private def denormalize(exercise: Double): String = {
    import SuggestionsJob._

    val foundIndex = binarySearch(weightedMuscleGroups.map(_._2).toArray, exercise)
    val insertIndex = -foundIndex - 1

    (if (insertIndex == 0) {
      weightedMuscleGroups.head
    } else if (insertIndex == weightedMuscleGroups.length) {
      weightedMuscleGroups.last
    } else if (foundIndex < 0) {
      weightedMuscleGroups(insertIndex - 1)
    } else {
      weightedMuscleGroups(foundIndex)
    })
    ._1.key
  }
}
