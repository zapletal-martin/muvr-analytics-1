package io.muvr.analytics.basic

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.UUID

import akka.analytics.cassandra.JournalKey
import io.muvr.exercise._
import org.apache.commons.io.FileUtils
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import scala.collection.JavaConversions._

trait TrainingExampleWriter {
  val Exercise_Id_Name_Separator = "/"

  def extractNameFrom(exerciseId: String) =
    exerciseId.split(Exercise_Id_Name_Separator).last

  def extractGroupFrom(exerciseId: String) =
    exerciseId.split(Exercise_Id_Name_Separator).last

  def writeExample(exerciseId: String, data: List[FusedSensorData]): Unit
}

/**
 * Write the examples to CSV files in the output directory. The writer will generate random UUIDs as filenames for the
 * different examples.
 */
case class CSVTrainingExampleWriter(outDir: String) extends TrainingExampleWriter {

  import com.github.tototoshi.csv._

  def writeExample(exerciseId: String, data: List[FusedSensorData]): Unit = {
    val id = UUID.randomUUID().toString
    val csvFile = new File(s"$outDir/$id.csv")
    writeExample(exerciseId, data, csvFile)
  }

  private def writeExample(exerciseId: String, data: List[FusedSensorData], target: File): Unit = {
    val exerciseName = extractNameFrom(exerciseId)
    val exerciseGroup = extractGroupFrom(exerciseId)
    val writer = CSVWriter.open(target)

    val csvData = data.flatMap(fsd ⇒ fsd.data).map {
      case Threed(x, y, z) ⇒ List(exerciseGroup, exerciseName, x, y, z)
    }

    writer.writeAll(csvData)
    writer.close()
  }
}

object TrainingMain {

  import SparkConfiguration._
  import cassandrax._

  val TrainingDataUser = "26dc5fbe-714f-407e-b04d-b76f1a825c40"
  val SlackerDataUser = "86274edb-f212-4f1e-96f9-37e20b4a7e5e"

  def groupExamplesByUser(input: RDD[(JournalKey, Any)]) = {
    input.flatMap { case (JournalKey(UserExerciseProcessorPersistenceId(userId), _, _), EntireResistanceExerciseSession(id, _, examples)) ⇒
      println(userId + " " + id + " " + examples.length)
      examples.map(e ⇒ userId → e)
    }.flatMap { case (userId, example) ⇒
      example.correct.map(correct ⇒ (userId, correct.resistanceExercise, example.fusedSensorData))
    }.groupBy { case (userId, exercise, data) ⇒
      userId
    }
  }

  def prepareCSVWriter(rootDir: String) = {
    val csvWriter = CSVTrainingExampleWriter(rootDir)

    FileUtils.deleteDirectory(new File(rootDir))
    Files.createDirectories(Paths.get(rootDir))

    csvWriter
  }

  def extractExerciseTrainingData() = {
    val csvWriter = prepareCSVWriter("spark-csv-dump")
    val sc = new SparkContext(sparkConf)
    val allExamples = sc.eventTable()

    groupExamplesByUser(allExamples)
      .foreach {
      case (userId, exercises) ⇒
        if (userId.id.toString == TrainingDataUser)
          exercises.foreach {
            case (_, exercise, data) ⇒
              csvWriter.writeExample(exercise.id, data)
          }

    }
  }

  def extractActivityTrainingData() = {
    val csvWriter = prepareCSVWriter("spark-csv-dump-slacking")
    val sc = new SparkContext(sparkConf)
    val allExamples = sc.eventTable()

    groupExamplesByUser(allExamples)
      .foreach {
      case (userId, exercises) ⇒
        println("------------ USER " + userId)
        exercises.groupBy(_._2).foreach {
          case (exerciseName, data) ⇒
            println(exerciseName + " - " + data.size)
        }
        userId.id.toString match {
          case TrainingDataUser ⇒
            exercises.foreach {
              case (userId, exercise, data) ⇒
                csvWriter.writeExample("/Exercise", data)
            }
          case SlackerDataUser ⇒
            exercises.foreach {
              case (userId, exercise, data) ⇒
                csvWriter.writeExample("/Slacking", data)
            }
          case _ ⇒
        }
    }
  }

  def main(args: Array[String]) {
    extractActivityTrainingData()

    System.exit(0)
  }
}
