package org.hathitrust.htrc.tools.featureextractor

import java.io.File

import com.gilt.gfc.time.Timer
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.hathitrust.htrc.data.{HtrcVolume, HtrcVolumeId}
import org.hathitrust.htrc.tools.featureextractor.Helper._
import org.hathitrust.htrc.tools.featureextractor.features.{EF, VolumeFeatures}
import org.hathitrust.htrc.tools.spark.errorhandling.ErrorAccumulator
import org.hathitrust.htrc.tools.spark.errorhandling.RddExtensions._
import play.api.libs.json.Json

import scala.io.{Codec, Source, StdIn}

/**
  * Extracts a set of features (such as ngram counts, POS tags, etc.) from the HathiTrust
  * corpus for aiding in conducting 'distant-reading' (aka non-consumptive) research.
  *
  * @author Boris Capitanu
  */

object Main {
  val appName: String = "feature-extractor"
  val supportedLanguages: Set[String] = Set("ar", "zh", "en", "fr", "de", "es")

  def stopSparkAndExit(sc: SparkContext, exitCode: Int = 0): Unit = {
    try {
      sc.stop()
    }
    finally {
      System.exit(exitCode)
    }
  }

  def main(args: Array[String]): Unit = {
    val conf = new Conf(args)
    val numPartitions = conf.numPartitions.toOption
    val numCores = conf.numCores.map(_.toString).getOrElse("*")
    val pairtreeRootPath = conf.pairtreeRootPath().toString
    val outputPath = conf.outputPath().toString
    val outputAsPairtree = conf.outputAsPairtree()
    val compress = conf.compress()
    val indent = conf.indent()
    val htids = conf.htids.toOption match {
      case Some(file) => Source.fromFile(file).getLines().toSeq
      case None => Iterator.continually(StdIn.readLine()).takeWhile(_ != null).toSeq
    }

    val sparkConf = new SparkConf()
    sparkConf.setAppName(appName)
    sparkConf.setIfMissing("spark.master", s"local[$numCores]")

    val spark = SparkSession.builder()
      .config(sparkConf)
      .getOrCreate()

    val sc = spark.sparkContext

    try {
      logger.info("Starting...")
      logger.debug(s"Using $numCores cores")

      val t0 = Timer.nanoClock()

      new File(outputPath).mkdirs()

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

      val pagesRDD = volumesRDD.flatMap { vol =>
        val id = vol.volumeId
        vol.structuredPages.map(id -> _)
      }

      val featuresRDD =
        pagesRDD
          .mapValues(PageFeatureExtractor.extractPageFeatures)
          .groupByKey()
          .mapValues(pages => VolumeFeatures(pages.toVector))

      featuresRDD.foreach { case (id, features) =>
        val ext = ".json" + (if (compress) ".bz2" else "")
        val efFileName = id.cleanId + ext
        val efOutputPath =
          if (outputAsPairtree)
            new File(outputPath, id.toPairtreeDoc.rootPath)
          else new File(outputPath)
        val efFile = new File(efOutputPath, efFileName)
        val ef = EF(id.uncleanId, features)
        writeJsonFile(Json.toJsObject(ef), efFile, compress, indent)
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