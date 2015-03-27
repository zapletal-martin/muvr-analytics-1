package com.eigengo.lift.spark.jobs.suggestions

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{Param, ParamMap, Params}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.rdd.RDDFunctions._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types._

import scala.collection.mutable.ArrayBuffer

object MuscleGroupKeysFeatureExtractor extends Params {
   val useHistoryParam = new Param[Int](this, "useHistory", "use history")
 }

/**
 * Extracts muscle group features from normalized muscle group history
 */
class MuscleGroupKeysFeatureExtractor extends Transformer {
   import MuscleGroupKeysFeatureExtractor._

   override def transform(dataset: DataFrame, paramMap: ParamMap): DataFrame = {
     import dataset.sqlContext.implicits._

     val useHistory = paramMap.get(useHistoryParam).get

     val labelAndFeatures = dataset
       .select("muscleGroupsKeysNormalized")
       .rdd
       .flatMap(_.getAs[ArrayBuffer[Double]](0))
       .sliding(useHistory + 1)
       .map(x => (x.head, Vectors.dense(x.tail)))
       .toDF("muscleGroupsKeysLabel", "muscleGroupsKeysFeatures")

     labelAndFeatures
   }

   @DeveloperApi
   override def transformSchema(schema: StructType, paramMap: ParamMap): StructType =
     StructType(Array(StructField("muscleGroupsKeysLabel", DoubleType, true), StructField("muscleGroupsKeysFeatures", VectorType.VectorUDT, true)))
 }
