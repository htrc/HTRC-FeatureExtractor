package org.hathitrust.htrc.tools.featureextractor

import java.io.InputStream
import java.nio.charset.CodingErrorAction

import edu.illinois.i3.scala.text.docstructure._
import org.w3c.dom.Element

import scala.io.{Codec, Source}

object HTRCPage {

  def apply(stream: InputStream, metsEntry: Element) = {
    val seq = metsEntry.getAttribute("SEQ")

    implicit val codec = Codec("UTF-8")
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

    val textLines = Source.fromInputStream(stream).getLines()
    val lines = textLines.zipWithIndex.map {
      case (text, idx) => new Line(text, idx, seq)
    }

    new HTRCPage(lines.toList, seq)
  }

}

/**
  * Object representing a page of an HTRC volume
  *
  * @param lines The lines of text on the page
  * @param pageSeq The page identifier (sequence number)
  */
class HTRCPage(lines: Seq[Line], pageSeq: String) extends Page(lines, pageSeq) {}