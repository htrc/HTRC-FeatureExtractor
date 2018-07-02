package org.hathitrust.htrc.tools.featureextractor

import com.cybozu.labs.langdetect.Language

/**
  * Trait representing the basic stats that will be recorded for a page
  */
trait BasicStats extends Serializable {
  val tokenCount: Int
  val lineCount: Int
  val emptyLineCount: Int
  val sentenceCount: Int

  override def toString: String =
    s"tc: $tokenCount, lc: $lineCount, elc: $emptyLineCount, sc: $sentenceCount"
}

object SectionStats {

  def apply(tokenCount: Int, lineCount: Int, emptyLineCount: Int, sentenceCount: Int,
            capAlphaSeq: Int,
            beginLineChars: Map[String, Int], endLineChars: Map[String, Int],
            tokenPosCount: Map[String, Map[String, Int]]) =
    new SectionStats(tokenCount, lineCount, emptyLineCount, sentenceCount,
      capAlphaSeq, beginLineChars, endLineChars, tokenPosCount)

  def unapply(section: SectionStats) = Some((
    section.tokenCount, section.lineCount, section.emptyLineCount, section.sentenceCount,
    section.capAlphaSeq,
    section.beginLineChars, section.endLineChars, section.tokenPosCount
    ))

}

/**
  * Object representing the set of stats recorded for a page section
  *
  * @param tokenCount The total token count for the section
  * @param lineCount The total line count for the section
  * @param emptyLineCount The empty line count for the section
  * @param sentenceCount The sentence count for the section
  * @param capAlphaSeq The longest sequence of lines that has the first character capitalized in
  *                    alphabetic order
  * @param beginLineChars The counts of characters occurring at the beginning of each line
  * @param endLineChars The counts of characters occurring at the end of each line
  * @param tokenPosCount The token counts for each token on the page, for each part-of-speech
  *                      tag it represents
  */
class SectionStats(val tokenCount: Int, val lineCount: Int, val emptyLineCount: Int, val sentenceCount: Int,
                   val capAlphaSeq: Int,
                   val beginLineChars: Map[String, Int], val endLineChars: Map[String, Int],
                   val tokenPosCount: Map[String, Map[String, Int]]) extends BasicStats


object PageStats {

  val SchemaVersion = "3.0"

  def apply(seq: String, headerStats: SectionStats, bodyStats: SectionStats, footerStats: SectionStats, languages: Seq[Language]) =
    new PageStats(seq, headerStats, bodyStats, footerStats, languages)

  def unapply(page: PageStats) = Some((
    page.seq, page.tokenCount, page.lineCount, page.emptyLineCount, page.sentenceCount,
    page.headerStats, page.bodyStats, page.footerStats, page.languages
    ))

}

/**
  * Object recording aggregate statistics at the page level
  *
  * @param seq The page sequence id
  * @param headerStats The page header stats
  * @param bodyStats The page body stats
  * @param footerStats The page footer stats
  * @param languages The sequence of languages identified
  */
class PageStats(val seq: String, val headerStats: SectionStats, val bodyStats: SectionStats, val footerStats: SectionStats, val languages: Seq[Language]) extends BasicStats {
  private val combinedStats = Seq(headerStats, bodyStats, footerStats)

  val tokenCount = combinedStats.foldLeft(0)(_ + _.tokenCount)
  val lineCount = combinedStats.foldLeft(0)(_ + _.lineCount)
  val emptyLineCount = combinedStats.foldLeft(0)(_ + _.emptyLineCount)
  val sentenceCount = combinedStats.foldLeft(0)(_ + _.sentenceCount)
}