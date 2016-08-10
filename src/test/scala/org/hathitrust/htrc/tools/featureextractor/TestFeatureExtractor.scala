package org.hathitrust.htrc.tools.featureextractor

import edu.illinois.i3.scala.nlp.{Language => NLPLanguage}
import edu.illinois.i3.scala.text.docstructure.{Line, PageStructureParser}
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, FlatSpec, _}


class TestFeatureExtractor extends FlatSpec
  with PropertyChecks
  with OptionValues
  with BeforeAndAfterAll
  with ParallelTestExecution {

  def nlpModelsResolver(s: String) = getClass.getResourceAsStream(s"/nlp-models/$s")

  val pageText = Seq[Line](
    new Line("  This is a title", 0, "1") { isHeader = true },
    new Line("that spans two lines", 1, "1") { isHeader = true },
    new Line("  ", 2, "1"),
    new Line("Once upon a time there was a fox", 3, "1"),
    new Line("that tried to jump, un-  ", 4, "1"),
    new Line("successfully, over an all-too-lazy dog.", 5, "1"),
    new Line("The dog didn't even notice the fox be-", 6, "1"),
    new Line("  cause he was sleeping soundly", 7, "1"),
    new Line("in his bed.", 8, "1"),
    new Line("I received a notice from the dog,", 9, "1"),
    new Line("complaining about this little fox.", 10, "1"),
    new Line("Sleeping soundly, the dog ignored the fox.", 11, "1"),
    new Line("The fox jumped, again, over the dog.", 12, "1"),
    new Line("Not again, complained the dog...", 13, "1")
  )

  val pages = PageStructureParser.parsePageStructure(
    Seq(new HTRCPage(pageText, "1"))
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    val langProfilesPath = getClass.getResource("/lang-profiles").getPath
    Executor.initializeLangDetect(langProfilesPath)
  }

  "countLongestAlphaSequenceOfCapitalizedLines" should "work correctly" in {
    val featureExtractor = FeatureExtractor(NLPLanguage.English, nlpModelsResolver)
    val count = featureExtractor.countLongestAlphaSequenceOfCapitalizedLines(pageText.map(_.text))
    count shouldBe 3
  }

  "computePageStats" should "return the correct set of features and statistics" in {
    val pageStats = Executor.computePageStats(pages.head, nlpModelsResolver)

    val headerStats = pageStats.headerStats
    val bodyStats = pageStats.bodyStats
    val footerStats = pageStats.footerStats

    headerStats.tokenCount should be (8)
    headerStats.lineCount should be (2)
    headerStats.emptyLineCount should be (0)
    headerStats.sentenceCount should be (1)
    headerStats.capAlphaSeq should be (1)
    headerStats.beginLineChars should equal (Map("t" -> 1, "T" -> 1))
    headerStats.endLineChars should equal (Map("e" -> 1, "s" -> 1))
    headerStats.tokenPosCount should equal (Map(
      "is" -> Map("VBZ" -> 1),
      "This" -> Map("DT" -> 1),
      "two" -> Map("CD" -> 1),
      "a" -> Map("DT" -> 1),
      "that" -> Map("WDT" -> 1),
      "spans" -> Map("VBZ" -> 1),
      "title" -> Map("NN" -> 1),
      "lines" -> Map("NNS" -> 1)
    ))

    bodyStats.tokenCount should be (77)
    bodyStats.lineCount should be (12)
    bodyStats.emptyLineCount should be (1)
    bodyStats.sentenceCount should be (6)
    bodyStats.capAlphaSeq should be (3)
    bodyStats.beginLineChars should equal (Map(
      "s" -> 1, "N" -> 1, "T" -> 2, "t" -> 1, "I" -> 1, "i" -> 1, "c" -> 2, "O" -> 1, "S" -> 1
    ))
    bodyStats.endLineChars should equal (Map("x" -> 1, "." -> 6, "y" -> 1, "-" -> 2, "," -> 1))
    bodyStats.tokenPosCount should equal (Map(
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
    ))

    footerStats.tokenCount should be (0)
    footerStats.lineCount should be (0)
    footerStats.emptyLineCount should be (0)
    footerStats.sentenceCount should be (0)
    footerStats.capAlphaSeq should be (0)
    footerStats.beginLineChars shouldBe empty
    footerStats.endLineChars shouldBe empty
    footerStats.tokenPosCount shouldBe empty

    pageStats.tokenCount should be (85)
    pageStats.lineCount should be (14)
    pageStats.emptyLineCount should be (1)
    pageStats.sentenceCount should be (7)
    pageStats.languages should have size 1
    pageStats.languages.head.lang should be ("en")
    pageStats.languages.head.prob should be >0.9d
  }
}
