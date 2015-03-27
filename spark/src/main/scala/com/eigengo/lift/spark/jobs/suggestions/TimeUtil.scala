package com.eigengo.lift.spark.jobs.suggestions

import java.util.Date

/**
 * Utilities for work with java Date
 */
object TimeUtil {
  /**
   * Add milliseconds to date
   * @param date input date
   * @param millis milliseconds
   * @return new date
   */
  def addMilliseconds(date: Date, millis: Long) =
    new Date(date.getTime + millis)

  /**
   * Add days to date
   * @param date input date
   * @param days number of days added/substracted
   */
  def addDays(date: Date, days: Int) =
    addMilliseconds(date, 3600000 * 24 * days)
}
