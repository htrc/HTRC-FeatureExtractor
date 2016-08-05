package org.hathitrust.htrc.tools.featureextractor
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile
import javax.xml.namespace.NamespaceContext
import javax.xml.xpath.{XPathConstants, XPathFactory}

import edu.illinois.i3.scala.text.docstructure.{Page, PageStructureParser, PageWithStructure}
import edu.illinois.i3.scala.utils.implicits.XmlImplicits._
import org.hathitrust.htrc.tools.pairtreehelper.PairtreeHelper.PairtreeDocument
import org.w3c.dom.{Element, NodeList}
import resource._

object HTRCDocument {

  def parse(pairtreeDoc: PairtreeDocument, pairtreeRoot: String):
      HTRCDocument[Page with PageWithStructure] = {

    val docRootPath = new File(pairtreeRoot, pairtreeDoc.getDocumentRootPath)
    val docZip = new File(docRootPath, pairtreeDoc.getCleanIdWithoutLibId + ".zip")
    val docMets = new File(docRootPath, pairtreeDoc.getCleanIdWithoutLibId + ".mets.xml")

    val volume = managed(new ZipFile(docZip, StandardCharsets.UTF_8)).map { zipFile =>
      val metsXml = Helper.loadXml(docMets)

      val xpath = XPathFactory.newInstance().newXPath()
      xpath.setNamespaceContext(new NamespaceContext {
        override def getPrefixes(namespaceURI: String) = null

        override def getPrefix(namespaceURI: String): String = null

        override def getNamespaceURI(prefix: String): String = prefix match {
          case "METS" => "http://www.loc.gov/METS/"
          case "xlink" => "http://www.w3.org/1999/xlink"
          case _ => null
        }
      })

      val metsOcrTxtFiles = xpath.evaluate(
        s"""//METS:fileGrp[@USE="ocr"]/METS:file[@MIMETYPE="text/plain"]/METS:FLocat""",
        metsXml, XPathConstants.NODESET).asInstanceOf[NodeList]

      val pages = metsOcrTxtFiles.toList.map { case metsTxt =>
        val txtFileName = metsTxt.asInstanceOf[Element].getAttribute("xlink:href")
        val txtZipPath = new File(pairtreeDoc.getCleanIdWithoutLibId, txtFileName).toString
        Option(zipFile.getEntry(txtZipPath)) match {
          case Some(zipEntry) =>
            val metsEntry = metsTxt.getParentNode.asInstanceOf[Element]
            HTRCPage(zipFile.getInputStream(zipEntry), metsEntry)

          case None =>
            throw HTRCPairtreeDocumentException(
              s"Could not find ZIP entry for: $txtFileName!", null)
        }
      }

      new HTRCDocument(pairtreeDoc, docRootPath.toString,
        PageStructureParser.parsePageStructure(pages))

    }.either match {
      case Right(vol) => vol
      case Left(errors) =>
        throw HTRCPairtreeDocumentException(
          s"Error processing document: ${pairtreeDoc.getUncleanId}", errors.head)
    }

    volume
  }
}

class HTRCDocument[T <: Page](val pairtreeDoc: PairtreeDocument,
                              val docRootPath: String,
                              val pages: Seq[T]) {
  override def toString: String =
    new File(docRootPath, pairtreeDoc.getCleanIdWithoutLibId + ".zip").toString
}

