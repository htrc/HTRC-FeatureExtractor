package org.hathitrust.htrc.tools.featureextractor

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import org.apache.commons.codec.digest.DigestUtils
import org.hathitrust.htrc.data.HtrcStructuredPage
import org.hathitrust.htrc.textprocessing.runningheaders.{Lines, Page}
import org.hathitrust.htrc.tools.featureextractor.LanguageDetector.detectLanguage
import org.hathitrust.htrc.tools.featureextractor.features.{PageFeatures, SectionFeatures}
import org.hathitrust.htrc.tools.featureextractor.stanfordnlp.NLPInstances

import scala.collection.JavaConverters._
import scala.util.matching.Regex

object PageFeatureExtractor {
  val hyphenWordRegex: Regex = """(\S*\p{L})-\n(\p{L}\S*)\s?""".r
  val punctBefore: Regex = """(?<=^|\s)(\p{P}+)(?=\p{L})""".r
  val punctAfter: Regex = """(?<=\p{L})(\p{P}+)(?=\s|$)""".r
  val maxTokenChars: Int = 200
  val posTagUnknown: String = "UNK"

  def countLongestAlphaSequenceOfCapitalizedLines(lines: Lines): Int = {
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

  def extractBasicFeatures(lines: Lines, nlp: StanfordCoreNLP): Option[SectionFeatures] = {
    // trim lines and filter out empty lines
    val nonEmptyLines = lines.map(_.trim).filterNot(_.isEmpty)
    val emptyLineCount = lines.size - nonEmptyLines.size

    // create the character distribution for begin and end characters on each line
    val beginCharCount = nonEmptyLines.groupBy(_.head.toString).mapValues(_.length).map(identity)
    val endCharCount = nonEmptyLines.groupBy(_.last.toString).mapValues(_.length).map(identity)

    // find the count of the longest sequence of lines starting with a capital letter in alphabetic order
    val longestAlphaSeq = countLongestAlphaSequenceOfCapitalizedLines(nonEmptyLines)

    val text = {
      var s = nonEmptyLines.mkString("\n")
      s = hyphenWordRegex.replaceAllIn(s, "$1$2\n") // combine hyphenated words occurring at end of line
      s = punctBefore.replaceAllIn(s, "$1 ")        // separate punctuation at beginning of words
      s = punctAfter.replaceAllIn(s, " $1")         // separate punctuation at end of words
      s
    }

    if (text.isEmpty)
      return None

    val annotatedText: Annotation = {
      val annotation = new Annotation(text)
      nlp.annotate(annotation)
      annotation
    }

    val tokenPos =
      annotatedText
        .get(classOf[CoreAnnotations.TokensAnnotation])
        .iterator()
        .asScala
        .map(token => token.originalText().take(maxTokenChars) -> posTagUnknown)
        .toList

    val tokenPosCount = tokenPos.groupBy { case (token, _) => token }.map {
      case (token, tokenPosArr) =>
        token ->
          tokenPosArr
            .map { case (_, pos) => pos }
            .groupBy(identity)
            .mapValues(_.length)
            .map(identity)
    }

    Some(SectionFeatures(
      tokenCount = tokenPos.length,
      lineCount = lines.length,
      emptyLineCount = emptyLineCount,
      sentenceCount = None,
      capAlphaSeq = longestAlphaSeq,
      beginCharCount = beginCharCount,
      endCharCount = endCharCount,
      tokenPosCount = tokenPosCount
    ))
  }

  def extractFullFeatures(lines: Lines, nlp: StanfordCoreNLP): Option[SectionFeatures] = {
    // trim lines and filter out empty lines
    val nonEmptyLines = lines.map(_.trim).filterNot(_.isEmpty)
    val emptyLineCount = lines.size - nonEmptyLines.size

    // create the character distribution for begin and end characters on each line
    val beginCharCount = nonEmptyLines.groupBy(_.head.toString).mapValues(_.length).map(identity)
    val endCharCount = nonEmptyLines.groupBy(_.last.toString).mapValues(_.length).map(identity)

    // find the count of the longest sequence of lines starting with a capital letter in alphabetic order
    val longestAlphaSeq = countLongestAlphaSequenceOfCapitalizedLines(nonEmptyLines)

    // combine hyphenated words occurring at end of line
    val text = hyphenWordRegex.replaceAllIn(nonEmptyLines.mkString("\n"), "$1$2\n")

    if (text.isEmpty)
      return None

    val annotatedText: Annotation = {
      val annotation = new Annotation(text)
      nlp.annotate(annotation)
      annotation
    }

    val sentenceCount = annotatedText.get(classOf[CoreAnnotations.SentencesAnnotation]).size()

    val tokenPos =
      annotatedText
        .get(classOf[CoreAnnotations.TokensAnnotation])
        .iterator()
        .asScala
        .map(token => token.originalText().take(maxTokenChars) -> token.tag())
        .toList

    val tokenPosCount = tokenPos.groupBy { case (token, _) => token }.map {
      case (token, tokenPosArr) => token -> tokenPosArr.map { case (_, pos) => pos }.groupBy(identity).mapValues(_.length).map(identity)
    }

    Some(SectionFeatures(
      tokenCount = tokenPos.length,
      lineCount = lines.length,
      emptyLineCount = emptyLineCount,
      sentenceCount = Some(sentenceCount),
      capAlphaSeq = longestAlphaSeq,
      beginCharCount = beginCharCount,
      endCharCount = endCharCount,
      tokenPosCount = tokenPosCount
    ))
  }

  def extractPageFeatures(page: HtrcStructuredPage): PageFeatures = {
    val text = page.asInstanceOf[Page].text
    val locale = detectLanguage(text)
    val version = DigestUtils.md5Hex(text)
    val (header, body, footer) =
      locale.flatMap(NLPInstances.forLocale) match {
        case Some(nlp) =>
          val headerFeatures = extractFullFeatures(page.headerLines, nlp)
          val bodyFeatures = extractFullFeatures(page.bodyLines, nlp)
          val footerFeatures = extractFullFeatures(page.footerLines, nlp)

          (headerFeatures, bodyFeatures, footerFeatures)

        case None =>
          val nlp = NLPInstances.whitespaceTokenizer
          val headerFeatures = extractBasicFeatures(page.headerLines, nlp)
          val bodyFeatures = extractBasicFeatures(page.bodyLines, nlp)
          val footerFeatures = extractBasicFeatures(page.footerLines, nlp)

          (headerFeatures, bodyFeatures, footerFeatures)
      }

    PageFeatures(
      seq = page.seq,
      version = version,
      language = locale.map(_.getLanguage),
      header = header,
      body = body,
      footer = footer
    )
  }
}