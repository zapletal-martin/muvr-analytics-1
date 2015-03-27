package com.eigengo.lift.spark.jobs.suggestions

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{ParamMap, _}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType

object UserFilter extends Params {
  val userIdParam = new Param[String](this, "userId", "user id")
}

/**
 * Filters single user from the journal history data
 */
class UserFilter extends Transformer {
  import UserFilter._

  override def transform(dataset: DataFrame, paramMap: ParamMap): DataFrame =
    dataset.filter(dataset("journalKey.persistenceId") === s"user-exercises-${paramMap.get(userIdParam).get}")

  @DeveloperApi
  override def transformSchema(schema: StructType, paramMap: ParamMap): StructType = schema
}
