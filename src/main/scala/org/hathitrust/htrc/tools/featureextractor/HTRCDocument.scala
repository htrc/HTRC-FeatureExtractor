package org.hathitrust.htrc.tools.featureextractor

import java.io._
import java.util.zip.ZipFile
import javax.xml.namespace.NamespaceContext
import javax.xml.xpath.{XPathConstants, XPathFactory}

import edu.illinois.i3.scala.text.docstructure.{Page, PageStructureParser, PageWithStructure}
import edu.illinois.i3.scala.utils.implicits.XmlImplicits._
import org.hathitrust.htrc.tools.pairtreehelper.PairtreeHelper.PairtreeDocument
import org.w3c.dom.{Element, NodeList}
import play.api.libs.json.{JsObject, Json}
import resource._

import scala.io.Codec

object HTRCDocument {

  /**
    * Checks if the given files exist and throw FileNotFoundException any are missing
    *
    * @param files The files to check
    */
  private def checkFilesExist(files: File*): Unit =
    files.filterNot(_.exists()).foreach(f =>
      throw new FileNotFoundException(f.toString + " (No such file or directory)")
    )

  /**
    * Loads and parses an HTRC volume from the Pairtree; parsing performs
    * header/footer identification
    *
    * @param pairtreeDoc The HTRC volume reference
    * @param pairtreeRoot The folder representing the Pairtree root structure
    * @param codec (Optional) The codec to use for reading/parsing the text
    * @return The parsed HTRCDocument
    */
  def parse(pairtreeDoc: PairtreeDocument, pairtreeRoot: String)(implicit codec: Codec):
      HTRCDocument[Page with PageWithStructure] = {

    val docRootPath = new File(pairtreeRoot, pairtreeDoc.getDocumentRootPath)
    val docZipFile = new File(docRootPath, pairtreeDoc.getCleanIdWithoutLibId + ".zip")
    val docMetsFile = new File(docRootPath, pairtreeDoc.getCleanIdWithoutLibId + ".mets.xml")
    val docMetaFile = new File(docRootPath, pairtreeDoc.getCleanIdWithoutLibId + ".json")

    checkFilesExist(docZipFile, docMetsFile, docMetaFile)

    val docMeta = managed(new FileInputStream(docMetaFile)).map(Json.parse).either match {
      case Right(meta) => meta
      case Left(errors) =>
        throw HTRCPairtreeDocumentException(
          s"[${pairtreeDoc.getUncleanId}] Error while retrieving document metadata", errors.head
        )
    }

    val volume = managed(new ZipFile(docZipFile, codec.charSet)).map { zipFile =>
      val metsXml = Helper.loadXml(docMetsFile)

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
              s"[${pairtreeDoc.getUncleanId}] Could not find ZIP entry for: $txtFileName", null)
        }
      }

      new HTRCDocument(pairtreeDoc, docMeta.as[JsObject],
        PageStructureParser.parsePageStructure(pages))

    }.either match {
      case Right(vol) => vol
      case Left(errors) =>
        throw HTRCPairtreeDocumentException(
          s"Error processing document: ${pairtreeDoc.getUncleanId}", errors.head
        )
    }

    volume
  }
}

/**
  * An object representing an HTRC document
  *
  * @param pairtreeDoc The pairtree reference object describing the document
  * @param metadata The JSON metadata entry for this document
  * @param pages The sequence of pages comprising this document
  * @tparam T Type parameter identifying the object type for the pages
  */
class HTRCDocument[T <: Page](val pairtreeDoc: PairtreeDocument,
                              val metadata: JsObject,
                              val pages: Seq[T]) {
  override def toString: String = pairtreeDoc.getUncleanId
}

