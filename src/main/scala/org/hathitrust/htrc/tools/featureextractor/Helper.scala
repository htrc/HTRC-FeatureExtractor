package org.hathitrust.htrc.tools.featureextractor

import java.io.{OutputStreamWriter, _}
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.log4j.Logger
import org.w3c.dom.Document
import play.api.libs.json.{JsValue, Json}
import resource._

object Helper {
  @transient lazy val logger = Logger.getLogger("FELogger")

  /**
    * Loads a file as an XML document
    *
    * @param f The file
    * @return The XML document
    */
  def loadXml(f: File): Document = {
    val factory = DocumentBuilderFactory.newInstance()
    factory.setNamespaceAware(true)
    val documentBuilder = factory.newDocumentBuilder()
    documentBuilder.parse(f)
  }

  /**
    * Writes a JSON object to file
    * @param json The JSON object
    * @param file The file to write to
    * @param compress True if compression is desired, False otherwise
    * @param indent True if output should be pretty-printed, False otherwise
    */
  def writeJsonFile(json: JsValue, file: File, compress: Boolean, indent: Boolean): Unit = {
    val parent = file.getParentFile
    if (parent != null) parent.mkdirs()

    val outputStream = {
      var stream: OutputStream = new BufferedOutputStream(new FileOutputStream(file))
      if (compress)
        stream = new BZip2CompressorOutputStream(stream, BZip2CompressorOutputStream.MAX_BLOCKSIZE)

      new OutputStreamWriter(stream, StandardCharsets.UTF_8)
    }

    val jsonTxt = if (indent) Json.prettyPrint(json) else Json.stringify(json)

    for (writer <- managed(outputStream))
      writer.write(jsonTxt)
  }
}