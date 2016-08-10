package org.hathitrust.htrc.tools.featureextractor

import com.cybozu.labs.langdetect.Language
import edu.illinois.i3.scala.nlp.NLPToolsFactory._
import edu.illinois.i3.scala.text.docstructure.{Line, Page, PageWithStructure}
import opennlp.tools.postag.POSTagger
import opennlp.tools.sentdetect.SentenceDetector
import opennlp.tools.tokenize.Tokenizer

object FeatureExtractor {
  import edu.illinois.i3.scala.nlp.Language.Language

  val HyphenWordRegex = """(?m)(\S*\p{L})-\n(\p{L}\S*)\s*""".r

  def apply(lang: Language, nlpModelResolver: NLPResourceResolver) = new FeatureExtractor {
    import edu.illinois.i3.scala.nlp.NLPToolsFactory

    private[this] val resolver: NLPResourceResolver = nlpModelResolver

    override protected val tokenizer = NLPToolsFactory(lang, resolver).newTokenizer().get
    override protected val posTagger = NLPToolsFactory(lang, resolver).newPOSTagger().get
    override protected val sentenceDetector = NLPToolsFactory(lang, resolver).newSentenceDetector().get
  }
}

trait FeatureExtractor {
  protected val sentenceDetector: SentenceDetector
  protected val tokenizer: Tokenizer
  protected val posTagger: POSTagger

  def extractFeatures(page: Page with PageWithStructure, languages: Option[Seq[Language]]): PageStats = {
    val headerStats = processSection(page.getHeader)
    val bodyStats = processSection(page.getBody)
    val footerStats = processSection(page.getFooter)

    PageStats(
      seq = page.pageSeq,
      headerStats = headerStats,
      bodyStats = bodyStats,
      footerStats = footerStats,
      languages = languages.getOrElse(Seq.empty[Language])
    )
  }

  def countLongestAlphaSequenceOfCapitalizedLines(lines: Seq[String]) = {
    import java.lang.Math.max

    var maxSeqCount, curSeqCount = 0
    var lastChar: Option[Char] = None

    for (c <- lines.withFilter(_.head.isUpper).map(_.head)) {
      lastChar match {
        case Some(char) if c >= char => curSeqCount += 1
        case None => curSeqCount = 1
        case _ =>
          maxSeqCount = max(curSeqCount, maxSeqCount)
          curSeqCount = 1
      }

      lastChar = Some(c)
    }

    maxSeqCount = max(curSeqCount, maxSeqCount)

    maxSeqCount
  }

  protected def processSection(lines: Seq[Line]) = {
    import FeatureExtractor.HyphenWordRegex

    // trim lines and filter out empty lines
    val nonEmptyLines = lines.map(_.text.trim).filterNot(_.isEmpty)
    val emptyLineCount = lines.size - nonEmptyLines.size

    // create the character distribution for begin and end characters on each line
    val beginLineCharCounts = nonEmptyLines.groupBy(_.head.toString).mapValues(_.length)
    val endLineCharCounts = nonEmptyLines.groupBy(_.last.toString).mapValues(_.length)

    // find the count of the longest sequence of lines starting with a capital letter in alphabetic order
    val longestAlphaSeq = countLongestAlphaSequenceOfCapitalizedLines(nonEmptyLines)

    // combine hyphenated words occurring at end of line
    val text = HyphenWordRegex.replaceAllIn(nonEmptyLines.mkString("\n"), "$1$2\n")

    // perform sentence detection and part-of-speech tagging, then count the number of (token, pos) occurrences
    val sentences = sentenceDetector.sentDetect(text)
    val sentenceCount = sentences.length
    val tokenPos = sentences.flatMap(s => {
      val tokens = tokenizer.tokenize(s)
      val tags = posTagger.tag(tokens)
      tokens.zip(tags)
    })
    val tokenPosCounts = tokenPos.groupBy { case (token, _) => token }.map {
      case (token, tokenPosArr) => token -> tokenPosArr.map { case (_, pos) => pos }.groupBy(identity).mapValues(_.length)
    }

    SectionStats(
      tokenCount = tokenPos.length,
      lineCount = lines.size,
      emptyLineCount = emptyLineCount,
      sentenceCount = sentenceCount,
      capAlphaSeq = longestAlphaSeq,
      beginCharCounts = beginLineCharCounts,
      endCharCount = endLineCharCounts,
      tokenPosCount = tokenPosCounts
    )
  }
}