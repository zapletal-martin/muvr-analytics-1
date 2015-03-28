package com.eigengo.lift.spark.jobs.suggestions

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{Param, ParamMap, Params}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.rdd.RDDFunctions._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types._

object IntensityFeatureExtractor extends Params {
  val useHistoryParam = new Param[Int](this, "useHistory", "use history")
}

/**
 * Extracts features from exercise intensity history data
 */
class IntensityFeatureExtractor extends Transformer {
  import IntensityFeatureExtractor._

  override def transform(dataset: DataFrame, paramMap: ParamMap): DataFrame = {
    import dataset.sqlContext.implicits._

    val useHistory = paramMap.get(useHistoryParam).get

    val labelAndFeatures = dataset
      .select("sessionProps.intendedIntensity")
      .rdd
      .map(_.getDouble(0))
      .sliding(useHistory + 1)
      .map(x => (x.head, Vectors.dense(x.tail)))
      .toDF("intensityLabel", "intensityFeatures")

    labelAndFeatures
  }

  @DeveloperApi
  override def transformSchema(schema: StructType, paramMap: ParamMap): StructType =
    StructType(Array(StructField("intensityLabel", DoubleType, true), StructField("intensityFeatures", VectorType.VectorUDT, true)))
}
