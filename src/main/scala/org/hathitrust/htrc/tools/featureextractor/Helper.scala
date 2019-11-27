package org.hathitrust.htrc.tools.featureextractor

import java.io._
import java.nio.charset.StandardCharsets

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsObject, Json}

import scala.language.reflectiveCalls
import scala.util.Try

object Helper {
  @transient lazy val logger: Logger = LoggerFactory.getLogger(Main.appName)

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

  def using[A, B <: {def close() : Unit}](closeable: B)(f: B => A): A =
    try {
      f(closeable)
    }
    finally {
      Try(closeable.close())
    }

}