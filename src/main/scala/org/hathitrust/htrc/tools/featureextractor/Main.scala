package org.hathitrust.htrc.tools.featureextractor

import java.io.{File, FileInputStream}

import com.gilt.gfc.time.Timer
import edu.illinois.i3.scala.utils.metrics.Timer._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}
import org.hathitrust.htrc.tools.featureextractor.Executor._
import org.hathitrust.htrc.tools.pairtreehelper.PairtreeHelper
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.rogach.scallop.ScallopConf
import play.api.libs.json.Json

import scala.io.{Codec, Source, StdIn}
import scala.util.{Failure, Try}

/**
  * Extracts a set of features (such as ngram counts, POS tags, etc.) from the HathiTrust
  * corpus for aiding in conducting 'distant-reading' (aka non-consumptive) research.
  *
  * @author Boris Capitanu
  */

object Main {
  val appName = "feature-extractor"

  def main(args: Array[String]): Unit = {
    val conf = new Conf(args)
    val numPartitions = conf.numPartitions.toOption
    val pairtreeRootPath = conf.pairtreeRootPath().toString
    val langProfilePath = conf.langProfilePath().toString
    val nlpModelsPath = conf.nlpModelsPath().toString
    val outputPath = conf.outputPath()
    val compress = conf.compress()
    val indent = conf.indent()
    val htids = conf.htids.toOption match {
      case Some(file) => Source.fromFile(file).getLines().toSeq
      case None => Iterator.continually(StdIn.readLine()).takeWhile(_ != null).toSeq
    }

    def nlpModelsResolver(name: String) = new FileInputStream(new File(nlpModelsPath, name))
    val dateTimeFormatter = DateTimeFormat forPattern "yyyy-MM-dd'T'HH:mm"
    val dateCreated = DateTime.now().toString(dateTimeFormatter)

    val sparkConf = new SparkConf()
    sparkConf.setIfMissing("spark.master", "local[*]")
    sparkConf.setIfMissing("spark.app.name", appName)

    val sc = new SparkContext(sparkConf)

    val (_, elapsed) = time {
      outputPath.mkdirs()

      val ids = numPartitions match {
        case Some(n) => sc.parallelize(htids, n)  // split input into n partitions
        case None => sc.parallelize(htids)        // use default number of partitions
      }

      // cache the ids (to be re-used later) and initialize the language detector
      ids.persist(StorageLevel.MEMORY_ONLY_SER)
      ids.foreachPartition(_ => initializeLangDetect(langProfilePath))

      // convert the ids to pairtree documents and load/parse them to identify running headers
      val pairtreeDocs = ids.map(id => Try(PairtreeHelper.getDocFromUncleanId(id)))
      val htrcDocs = pairtreeDocs.map(_.map(HTRCDocument.parse(_, pairtreeRootPath)(Codec.UTF8)))

      // run the feature extractor on each page, for each volume, and return the result as JSON
      val docStats = htrcDocs.map(_.map { doc =>
        val pagesStats = doc.pages.map(computePageStats(_, nlpModelsResolver))

        import JsonStats._

        doc.pairtreeDoc -> Json.obj(
          "id" -> doc.pairtreeDoc.getUncleanId,
          "metadata" -> doc.metadata,
          "features" -> Json.toJsFieldJsValueWrapper(Json.obj(
            "schemaVersion" -> PageStats.SchemaVersion,
            "dateCreated" -> dateCreated,
            "pageCount" -> pagesStats.size,
            "pages" -> Json.toJson(pagesStats)
          ))
        )
      })

      // save the JSON results in a pairtree structure
      val results = docStats.map(_.map { case (doc, json) =>
        val ext = "json" + (if (compress) ".bz2" else "")
        val outputFile = new File(outputPath, s"${doc.getDocumentRootPath}/${doc.getCleanId}.$ext")
        Helper.writeJsonFile(json, outputFile, compress, indent)
        doc.getUncleanId
      })

      // retrieve information about any documents that have failed to be processed
      val failed = ids.zip(results)
        .filter(_._2.isFailure)
        .map {
          case (id, Failure(e)) =>
            val cause = Option(e.getCause).getOrElse(e)
            s"$id\t${cause.getMessage}"

          case _ => ""
        }

      // ... and save that to disk for inspection
      failed.saveAsTextFile(s"$outputPath/errors")
    }

    logger.info(f"All done in ${Timer.pretty(elapsed*1e6.toLong)}")
  }

}

/**
  * Command line argument configuration
  *
  * @param arguments The cmd line args
  */
class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val (appTitle, appVersion, appVendor) = {
    val p = getClass.getPackage
    val nameOpt = Option(p).flatMap(p => Option(p.getImplementationTitle))
    val versionOpt = Option(p).flatMap(p => Option(p.getImplementationVersion))
    val vendorOpt = Option(p).flatMap(p => Option(p.getImplementationVendor))
    (nameOpt, versionOpt, vendorOpt)
  }

  version(appTitle.flatMap(
    name => appVersion.flatMap(
      version => appVendor.map(
        vendor => s"$name $version\n$vendor"))).getOrElse(Main.appName))

  val numPartitions = opt[Int]("num-partitions",
    descr = "The number of partitions to split the input set of HT IDs into, " +
      "for increased parallelism",
    required = false,
    argName = "N",
    validate = 0<
  )

  val pairtreeRootPath = opt[File]("pairtree",
    descr = "The path to the paitree root hierarchy to process",
    required = true,
    argName = "DIR"
  )

  val langProfilePath = opt[File]("lang-dir",
    descr = "The path to the language profiles",
    required = true,
    argName = "DIR"
  )

  val nlpModelsPath = opt[File]("nlp-models-dir", short = 'm',
    descr = "The path to the NLP models",
    required = true,
    argName = "DIR"
  )

  val outputPath = opt[File]("output",
    descr = "Write the output to DIR (should not exist, or be empty)",
    required = true,
    argName = "DIR"
  )

  val compress = opt[Boolean]("compress",
    descr = "Compress the output"
  )

  val indent = opt[Boolean]("indent",
    descr = "Indent the output"
  )

  val htids = trailArg[File]("htids",
    descr = "The file containing the HT IDs to be searched (if not provided, will read from stdin)",
    required = false
  )

  validateFileExists(pairtreeRootPath)
  validateFileExists(langProfilePath)
  validateFileExists(nlpModelsPath)
  validateFileExists(htids)
  verify()
}