package org.hathitrust.htrc.tools.featureextractor

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsObject, Json}

import java.io._
import java.nio.charset.StandardCharsets
import scala.language.reflectiveCalls
import scala.util.Using

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
      @SuppressWarnings(Array("org.wartremover.warts.Var"))
      var stream: OutputStream = new BufferedOutputStream(new FileOutputStream(file))
      if (compress)
        stream = new BZip2CompressorOutputStream(stream, BZip2CompressorOutputStream.MAX_BLOCKSIZE)

      new OutputStreamWriter(stream, StandardCharsets.UTF_8)
    }

    val jsonTxt = if (indent) Json.prettyPrint(json) else Json.stringify(json)

    Using.resource(outputStream)(_.write(jsonTxt))
  }

}