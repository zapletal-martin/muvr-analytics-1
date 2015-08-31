package io.muvr.analytics.basic

import org.apache.spark.SparkContext

object DiagnosticMain {
  import SparkConfiguration._
  import cassandrax._

  def main(args: Array[String]) {
    val sc = new SparkContext(sparkConf)
    sc.eventTable().foreach(evt ⇒ println("*** :) " + evt))
  }
}
