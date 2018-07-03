package org.hathitrust.htrc.tools.featureextractor

import java.io._
import java.nio.charset.StandardCharsets
import java.util.{Locale, Properties}

import com.optimaize.langdetect.i18n.LdLocale
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.hathitrust.htrc.tools.scala.io.IOUtils.using
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsObject, Json}

import scala.util.{Failure, Try}

object Helper {
  @transient lazy val logger: Logger = LoggerFactory.getLogger(Main.appName)

  def loadPropertiesFromClasspath(path: String): Try[Properties] = {
    require(path != null && path.nonEmpty)

    Option(getClass.getResourceAsStream(path))
      .map(using(_) { is =>
        Try {
          val props = new Properties()
          props.load(is)
          props
        }
      })
      .getOrElse(Failure(new FileNotFoundException(s"$path not found")))
  }

  /**
    * Writes a JSON object to file
    *
    * @param json     The JSON object
    * @param file     The file to write to
    * @param compress True if compression is desired, False otherwise
    * @param indent   True if output should be pretty-printed, False otherwise
    */
  def writeJsonFile(json: JsObject, file: File, compress: Boolean, indent: Boolean): Unit = {
    val parent = file.getParentFile
    if (parent != null) parent.mkdirs()

    val outputStream = {
      var stream: OutputStream = new BufferedOutputStream(new FileOutputStream(file))
      if (compress)
        stream = new BZip2CompressorOutputStream(stream, BZip2CompressorOutputStream.MAX_BLOCKSIZE)

      new OutputStreamWriter(stream, StandardCharsets.UTF_8)
    }

    val jsonTxt = if (indent) Json.prettyPrint(json) else Json.stringify(json)

    using(outputStream)(_.write(jsonTxt))
  }

  implicit class LdLocaleDecorator(ldLocale: LdLocale) {
    def toLocale: Locale = {
      val builder = new Locale.Builder()
      builder.setLanguage(ldLocale.getLanguage)
      if (ldLocale.getRegion.isPresent)
        builder.setRegion(ldLocale.getRegion.get())
      if (ldLocale.getScript.isPresent)
        builder.setScript(ldLocale.getScript.get())
      builder.build()
    }
  }
}