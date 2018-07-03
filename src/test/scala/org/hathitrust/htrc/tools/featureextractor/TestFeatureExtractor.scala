package org.hathitrust.htrc.tools.featureextractor

import org.hathitrust.htrc.data.HtrcStructuredPage
import org.hathitrust.htrc.textprocessing.runningheaders.Lines
import org.hathitrust.htrc.tools.featureextractor.PageFeatureExtractor._
import org.scalatest._
import org.scalatest.prop.PropertyChecks

class TestFeatureExtractor extends FlatSpec
  with PropertyChecks
  with OptionValues
  with Matchers
  with ParallelTestExecution {

  val pageLines: Lines = IndexedSeq[String](
    "  This is a title",
    "that spans two lines",
    "  ",
    "Once upon a time there was a fox",
    "that tried to jump, un-  ",
    "successfully, over an all-too-lazy dog.",
    "The dog didn't even notice the fox be-",
    "  cause he was sleeping soundly",
    "in his bed.",
    "I received a notice from the dog,",
    "complaining about this little fox.",
    "Sleeping soundly, the dog ignored the fox.",
    "The fox jumped, again, over the dog.",
    "Not again, complained the dog..."
  )

  val page: HtrcStructuredPage =
    new HtrcStructuredPage(
      seq = "1",
      textLines = pageLines,
      numHeaderLines = 2,
      numFooterLines = 0
    )

  "countLongestAlphaSequenceOfCapitalizedLines" should "work correctly" in {
    countLongestAlphaSequenceOfCapitalizedLines(pageLines) shouldBe 3
  }

  "extractPageFeatures" should "return the correct set of features" in {
    val pageFeatures = extractPageFeatures(page)
    pageFeatures.language shouldBe Some("en")
    pageFeatures.seq shouldBe "1"

    val headerFeatures = pageFeatures.header.value
    val bodyFeatures = pageFeatures.body.value

    headerFeatures.tokenCount shouldBe 8
    headerFeatures.lineCount shouldBe 2
    headerFeatures.emptyLineCount shouldBe 0
    headerFeatures.sentenceCount.value shouldBe 1
    headerFeatures.capAlphaSeq shouldBe 1
    headerFeatures.beginCharCount shouldBe Map("t" -> 1, "T" -> 1)
    headerFeatures.endCharCount shouldBe Map("e" -> 1, "s" -> 1)
    headerFeatures.tokenPosCount shouldBe Map(
      "is" -> Map("VBZ" -> 1),
      "This" -> Map("DT" -> 1),
      "two" -> Map("CD" -> 1),
      "a" -> Map("DT" -> 1),
      "that" -> Map("WDT" -> 1),
      "spans" -> Map("VBZ" -> 1),
      "title" -> Map("NN" -> 1),
      "lines" -> Map("NNS" -> 1)
    )

    bodyFeatures.tokenCount shouldBe 77
    bodyFeatures.lineCount shouldBe 12
    bodyFeatures.emptyLineCount shouldBe 1
    bodyFeatures.sentenceCount.value shouldBe 6
    bodyFeatures.capAlphaSeq shouldBe 3
    bodyFeatures.beginCharCount shouldBe Map(
      "s" -> 1, "N" -> 1, "T" -> 2, "t" -> 1, "I" -> 1, "i" -> 1, "c" -> 2, "O" -> 1, "S" -> 1
    )
    bodyFeatures.endCharCount shouldBe Map("x" -> 1, "." -> 6, "y" -> 1, "-" -> 2, "," -> 1)
    bodyFeatures.tokenPosCount shouldBe Map(
      "Once" -> Map("RB" -> 1),
      "this" -> Map("DT" -> 1),
      "in" -> Map("IN" -> 1),
      "his" -> Map("PRP$" -> 1),
      "jumped" -> Map("VBD" -> 1),
      "soundly" -> Map("RB" -> 2),
      "." -> Map("." -> 5),
      "jump" -> Map("VB" -> 1),
      "complained" -> Map("VBD" -> 1),
      "Sleeping" -> Map("VBG" -> 1),
      "a" -> Map("DT" -> 3),
      "because" -> Map("IN" -> 1),
      "complaining" -> Map("VBG" -> 1),
      "dog" -> Map("NN" -> 6),
      "I" -> Map("PRP" -> 1),
      "that" -> Map("WDT" -> 1),
      "upon" -> Map("IN" -> 1),
      "to" -> Map("TO" -> 1),
      "bed" -> Map("NN" -> 1),
      "did" -> Map("VBD" -> 1),
      "," -> Map("," -> 7),
      "was" -> Map("VBD" -> 2),
      "there" -> Map("EX" -> 1),
      "The" -> Map("DT" -> 2),
      "over" -> Map("IN" -> 2),
      "notice" -> Map("NN" -> 1, "VB" -> 1),
      "all-too-lazy" -> Map("JJ" -> 1),
      "unsuccessfully" -> Map("RB" -> 1),
      "he" -> Map("PRP" -> 1),
      "even" -> Map("RB" -> 1),
      "little" -> Map("JJ" -> 1),
      "again" -> Map("RB" -> 2),
      "from" -> Map("IN" -> 1),
      "Not" -> Map("RB" -> 1),
      "tried" -> Map("VBD" -> 1),
      "an" -> Map("DT" -> 1),
      "..." -> Map(":" -> 1),
      "time" -> Map("NN" -> 1),
      "ignored" -> Map("VBD" -> 1),
      "sleeping" -> Map("VBG" -> 1),
      "about" -> Map("IN" -> 1),
      "n't" -> Map("RB" -> 1),
      "fox" -> Map("NN" -> 5),
      "received" -> Map("VBD" -> 1),
      "the" -> Map("DT" -> 6)
    )

    pageFeatures.footer shouldBe None

    pageFeatures.tokenCount shouldBe 85
    pageFeatures.lineCount shouldBe 14
    pageFeatures.emptyLineCount shouldBe 1
    pageFeatures.sentenceCount.value shouldBe 7
  }
}
