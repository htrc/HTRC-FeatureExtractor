package org.hathitrust.htrc.tools.featureextractor

import java.io.File

import org.rogach.scallop.{Scallop, ScallopConf, ScallopHelpFormatter, ScallopOption, SimpleOption}

/**
  * Command line argument configuration
  *
  * @param arguments The cmd line args
  */
class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  appendDefaultToDescription = true
  helpFormatter = new ScallopHelpFormatter {
    override def getOptionsHelp(s: Scallop): String = {
      super.getOptionsHelp(s.copy(opts = s.opts.map {
        case opt: SimpleOption if !opt.required =>
          opt.copy(descr = "(Optional) " + opt.descr)
        case other => other
      }))
    }
  }

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

  val numPartitions: ScallopOption[Int] = opt[Int]("num-partitions",
    descr = "The number of partitions to split the input set of HT IDs into, " +
      "for increased parallelism",
    argName = "N",
    validate = 0<
  )

  val sparkLog: ScallopOption[String] = opt[String]("spark-log",
    descr = "Where to write logging output from Spark to",
    argName = "FILE",
    noshort = true
  )

  val logLevel: ScallopOption[String] = opt[String]("log-level",
    descr = "The application log level; one of INFO, DEBUG, OFF",
    argName = "LEVEL",
    default = Some("INFO"),
    validate = level => Set("INFO", "DEBUG", "OFF").contains(level.toUpperCase)
  )

  val numCores: ScallopOption[Int] = opt[Int]("num-cores",
    descr = "The number of CPU cores to use (if not specified, uses all available cores)",
    short = 'c',
    argName = "N",
    validate = 0 <
  )

  val pairtreeRootPath: ScallopOption[File] = opt[File]("pairtree",
    descr = "The path to the paitree root hierarchy to process",
    required = true,
    argName = "DIR"
  )

  val outputPath: ScallopOption[File] = opt[File]("output",
    descr = "Write the output to DIR (should not exist, or be empty)",
    required = true,
    argName = "DIR"
  )

  val saveAsSeqFile: ScallopOption[Boolean] = opt[Boolean]("save-as-seq",
    descr = "Saves the EF files as Hadoop sequence files",
    required = false,
    default = Some(false)
  )

//  val outputAsPairtree: ScallopOption[Boolean] = opt[Boolean]("out-pairtree",
//    descr = "Saves the EF files in a pairtree folder hierarchy",
//    noshort = true
//  )

  val htids: ScallopOption[File] = trailArg[File]("htids",
    descr = "The file containing the HT IDs to be searched (if not provided, will read from stdin)",
    required = false
  )

  validateFileIsDirectory(pairtreeRootPath)
  validateFileDoesNotExist(outputPath)
  validateFileExists(htids)
  verify()
}