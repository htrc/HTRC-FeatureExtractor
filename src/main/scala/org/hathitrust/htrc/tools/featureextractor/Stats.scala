package org.hathitrust.htrc.tools.featureextractor

import com.cybozu.labs.langdetect.Language

trait BasicStats extends Serializable {
  val tokenCount: Int
  val lineCount: Int
  val emptyLineCount: Int
  val sentenceCount: Int

  override def toString: String = s"tc: $tokenCount, lc: $lineCount, elc: $emptyLineCount, sc: $sentenceCount"

}

object SectionStats {

  def apply(tokenCount: Int, lineCount: Int, emptyLineCount: Int, sentenceCount: Int,
            capAlphaSeq: Int,
            beginCharCounts: Map[String, Int], endCharCount: Map[String, Int],
            tokenPosCount: Map[String, Map[String, Int]]) =
    new SectionStats(tokenCount, lineCount, emptyLineCount, sentenceCount,
      capAlphaSeq, beginCharCounts, endCharCount, tokenPosCount)

  def unapply(section: SectionStats) = Some((
    section.tokenCount, section.lineCount, section.emptyLineCount, section.sentenceCount,
    section.capAlphaSeq,
    section.beginLineChars, section.endLineChars, section.tokenPosCount
    ))

}

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

class PageStats(val seq: String, val headerStats: SectionStats, val bodyStats: SectionStats, val footerStats: SectionStats, val languages: Seq[Language]) extends BasicStats {
  private val combinedStats = Seq(headerStats, bodyStats, footerStats)

  val tokenCount = combinedStats.foldLeft(0)(_ + _.tokenCount)
  val lineCount = combinedStats.foldLeft(0)(_ + _.lineCount)
  val emptyLineCount = combinedStats.foldLeft(0)(_ + _.emptyLineCount)
  val sentenceCount = combinedStats.foldLeft(0)(_ + _.sentenceCount)
}