package com.eigengo.lift.spark.jobs.suggestions

import java.util.Date

object TimeUtil {
  def addMilliseconds(date: Date, millis: Long) =
    new Date(date.getTime + millis)


  def addDays(date: Date, days: Int) =
    addMilliseconds(date, 3600000 * 24 * days)
}
