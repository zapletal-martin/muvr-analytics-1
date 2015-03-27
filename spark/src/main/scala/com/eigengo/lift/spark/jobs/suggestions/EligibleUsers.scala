package com.eigengo.lift.spark.jobs.suggestions

import java.util.{UUID, Date}

import akka.analytics.cassandra.JournalKey
import com.eigengo.lift.exercise.UserExercises.SessionStartedEvt
import org.apache.spark.rdd.RDD

import scala.util.Try

//TODO: Move to a separate job?
object EligibleUsers {

  def getEligibleUsers(events: RDD[(JournalKey, Any)], sessionEndedBefore: Long): RDD[String] = {
    events
      .flatMap {
      case (k, e) if e.isInstanceOf[SessionStartedEvt] => Some((k, e.asInstanceOf[SessionStartedEvt]))
      case _ => None
    }
      .filter(ke => ke._2.sessionProps.startDate.compareTo(TimeUtil.addMilliseconds(new Date(), -sessionEndedBefore)) > 0)
      .map(_._1)
      .map(e => Try(UUID.fromString(e.persistenceId.takeRight(36))))
      .filter(_.isSuccess)
      .map(_.get.toString)
      .distinct()
  }
}
