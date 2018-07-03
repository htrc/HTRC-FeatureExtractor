package org.hathitrust.htrc.tools.featureextractor.features

object SectionFeatures {
  val empty: SectionFeatures =
    SectionFeatures(
      tokenCount = 0,
      lineCount = 0,
      emptyLineCount = 0,
      sentenceCount = Some(0),
      capAlphaSeq = 0,
      beginCharCount = Map.empty[String, Int],
      endCharCount = Map.empty[String, Int],
      tokenPosCount = Map.empty[String, Map[String, Int]]
    )
}

/**
  * Object representing the set of features recorded for a page section
  *
  * @param tokenCount     The total token count for the section
  * @param lineCount      The total line count for the section
  * @param emptyLineCount The empty line count for the section
  * @param sentenceCount  The sentence count for the section
  * @param capAlphaSeq    The longest sequence of lines that has the first character capitalized in
  *                       alphabetic order
  * @param beginCharCount The counts of characters occurring at the beginning of each line
  * @param endCharCount   The counts of characters occurring at the end of each line
  * @param tokenPosCount  The token counts for each token on the page, for each part-of-speech
  *                       tag it represents
  */
case class SectionFeatures(tokenCount: Int,
                           lineCount: Int,
                           emptyLineCount: Int,
                           sentenceCount: Option[Int],
                           capAlphaSeq: Int,
                           beginCharCount: Map[String, Int],
                           endCharCount: Map[String, Int],
                           tokenPosCount: Map[String, Map[String, Int]]) extends BasicFeatures
