package com.eigengo.lift.spark.jobs.suggestions

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

import scala.collection.mutable.ArrayBuffer

/**
 * Normalizes muscle group keys strings to double representation <0, 1>
 */
class MuscleGroupsKeysNormalizer extends Transformer {
  override def transform(dataset: DataFrame, paramMap: ParamMap): DataFrame =
    dataset
      .withColumn("muscleGroupsKeysNormalized", callUDF((k: ArrayBuffer[String]) => k.map(normalize(_)), ArrayType(DoubleType), dataset("sessionProps.muscleGroupsKeys")))

  @DeveloperApi
  override def transformSchema(schema: StructType, paramMap: ParamMap): StructType =
    StructType(schema.fields
      :+ StructField("muscleGroupsKeysNormalized", ArrayType(DoubleType), true))

  private def normalize(exercise: String): Double = {
    import SuggestionsJob._

    weightedMuscleGroups.find(x => x._1.key.compareToIgnoreCase(exercise) == 0).head._2
  }
}
