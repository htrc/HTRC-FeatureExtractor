package org.hathitrust.htrc.tools.featureextractor

import com.gilt.gfc.time.Timer
import org.apache.commons.io.FileUtils
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.hathitrust.htrc.data.{HtrcVolume, HtrcVolumeId}
import org.hathitrust.htrc.tools.featureextractor.Helper._
import org.hathitrust.htrc.tools.featureextractor.features.{EF, VolumeFeatures}
import org.hathitrust.htrc.tools.spark.errorhandling.ErrorAccumulator
import org.hathitrust.htrc.tools.spark.errorhandling.RddExtensions._
import org.hathitrust.htrc.tools.spark.utils.Helper.stopSparkAndExit
import play.api.libs.json.Json

import java.io.File
import java.nio.charset.StandardCharsets
import scala.io.{Codec, Source, StdIn}
import scala.util.Using

/**
  * Extracts a set of features (such as ngram counts, POS tags, etc.) from the HathiTrust
  * corpus for aiding in conducting 'distant-reading' (aka non-consumptive) research.
  *
  * @author Boris Capitanu
  */

object Main {
  val appName: String = "extract-features"
  val supportedLanguages: Set[String] = Set("ar", "zh", "en", "fr", "de", "es")

  @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
  def main(args: Array[String]): Unit = {
    val conf = new Conf(args.toIndexedSeq)
    val numPartitions = conf.numPartitions.toOption
    val numCores = conf.numCores.map(_.toString).getOrElse("*")
    val pairtreeRootPath = conf.pairtreeRootPath().toString
    val outputPath = conf.outputPath().toString
    val saveAsSeqFile = conf.saveAsSeqFile()
    val htids = conf.htids.toOption match {
      case Some(file) => Using.resource(Source.fromFile(file))(_.getLines().toList)
      case None => Iterator.continually(StdIn.readLine()).takeWhile(_ != null).toSeq
    }

    val featuresOutputPath = new File(outputPath, "features").toString

    // set up logging destination
    conf.sparkLog.foreach(System.setProperty("spark.logFile", _))
    System.setProperty("logLevel", conf.logLevel().toUpperCase)

    val sparkConf = new SparkConf()
    sparkConf.setAppName(appName)
    sparkConf.setIfMissing("spark.master", s"local[$numCores]")
    val sparkMaster = sparkConf.get("spark.master")

    val spark = SparkSession.builder()
      .config(sparkConf)
      .getOrCreate()

    val sc = spark.sparkContext

    try {
      logger.info("Starting...")
      logger.info(s"Spark master: $sparkMaster")

      val t0 = Timer.nanoClock()

      conf.outputPath().mkdirs()

      val idsRDD = numPartitions match {
        case Some(n) => sc.parallelize(htids, n) // split input into n partitions
        case None => sc.parallelize(htids) // use default number of partitions
      }

      val volumeErrAcc = new ErrorAccumulator[String, String](identity)(sc)
      val volumesRDD = idsRDD.tryMap { id =>
        val pairtreeVolume =
          HtrcVolumeId
            .parseUnclean(id)
            .map(_.toPairtreeDoc(pairtreeRootPath))
            .get

        HtrcVolume.from(pairtreeVolume)(Codec.UTF8).get
      }(volumeErrAcc)

      val featureExtractorErrAcc = new ErrorAccumulator[HtrcVolume, String](_.volumeId.uncleanId)(sc)
      val featuresRDD =
        volumesRDD
          .tryMap { vol =>
            val id = vol.volumeId
            val pages = vol.structuredPages
            val pagesFeatures = pages.map(HtrcPageFeatureExtractor.extractPageFeatures)
            id -> VolumeFeatures(id.uncleanId, pagesFeatures)
          }(featureExtractorErrAcc)

      if (saveAsSeqFile) {
        val featuresJsonRDD = featuresRDD
          .map { case (id, features) => id.uncleanId -> Json.toJson(features).toString }

        // the saving as BZip2Codec below resulted in errors when parsing the generated sequence files
        // the error was related to  org.apache.hadoop.io.compress.bzip2.CBZip2InputStream about offset out of bounds
        // this could be because of the built-in hadoop libs
//        featuresJsonRDD.saveAsSequenceFile(featuresOutputPath, Some(classOf[org.apache.hadoop.io.compress.BZip2Codec]))
        featuresJsonRDD.saveAsSequenceFile(featuresOutputPath)
      } else {
        val doneIds = featuresRDD.map { case (id, features) =>
          val efFileName = id.cleanId + ".json"
          val efOutputPath = new File(featuresOutputPath)
          val efFile = new File(efOutputPath, efFileName)
          val ef = EF(id.uncleanId, features)
          FileUtils.writeStringToFile(efFile, Json.prettyPrint(Json.toJson(ef)), StandardCharsets.UTF_8)
          id.uncleanId
        }

        doneIds.saveAsTextFile(new File(outputPath, "ids-done").toString)
      }

      if (volumeErrAcc.nonEmpty || featureExtractorErrAcc.nonEmpty) {
        logger.info("Writing error report(s)...")
        if (volumeErrAcc.nonEmpty)
          volumeErrAcc.saveErrors(new Path(outputPath, "id_errors.txt"))
        if (featureExtractorErrAcc.nonEmpty)
          featureExtractorErrAcc.saveErrors(new Path(outputPath, "extractor_errors.txt"))
      }

      val t1 = Timer.nanoClock()
      val elapsed = t1 - t0

      logger.info(f"All done in ${Timer.pretty(elapsed)}")
    }
    catch {
      case e: Throwable =>
        logger.error(s"Uncaught exception", e)
        stopSparkAndExit(sc, exitCode = 500)
    }

    stopSparkAndExit(sc)
  }
}