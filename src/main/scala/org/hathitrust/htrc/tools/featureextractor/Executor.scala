package org.hathitrust.htrc.tools.featureextractor

import com.cybozu.labs.langdetect.{DetectorFactory, Language}
import edu.illinois.i3.scala.nlp.NLPToolsFactory.NLPResourceResolver
import edu.illinois.i3.scala.nlp.{Language => NLPLanguage}
import org.apache.log4j.Logger
import org.hathitrust.htrc.textprocessing.runningheaders.{Page, PageWithStructure}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

object Executor {

  @transient lazy val logger = Logger.getLogger("FeatureExtractor")

  @transient var initialized = false
  @transient val lock = new Object()

  /**
    * Initializes the language detector by loading the language models in memory
    *
    * @param langDir The folder where the language models reside
    */
  def initializeLangDetect(langDir: String): Unit =  {
    if (!initialized) lock.synchronized {
      Try(DetectorFactory.loadProfile(langDir)) match {
        case Success(_) => logger.info(s"Loaded language profiles from $langDir")
        case Failure(_) =>
      }

      initialized = true
    }
  }

  val defaultLanguage = NLPLanguage.English

  /**
    * Performs language detection on a given text
    *
    * @param text The text
    * @return The most likely language(s) with probabilities, or None if couldn't determine
    */
  def detectLanguages(text: String): Option[Seq[Language]] = Try {
    val languageDetector = DetectorFactory.create()
    languageDetector.append(text)
    languageDetector.getProbabilities
  } match {
    case Success(langProbs) => Some(langProbs.toSeq)
    case _ => None
  }

  /**
    * Performs feature extraction on a page, using the specified NLP models
    *
    * @param p The page
    * @param nlpResResolver The NLP models resolver
    * @return The extracted features
    */
  def computePageStats(p: Page with PageWithStructure,
                       nlpResResolver: NLPResourceResolver): PageStats = {
    val languages = {
      // make sure the language detection is accurate - check that there are sufficient words
      // of >3 characters as context for the language detector, otherwise the results are
      // unreliable
      val wordsOnPage = p.getBody.map(_.text.trim).withFilter(_.nonEmpty)
        .flatMap(_.split( """\s+""").map(_.replaceAll("""[\.,!?;:")]+$""", "")))

      if (wordsOnPage.count(_.length > 3) < 10)
        None
      else detectLanguages(wordsOnPage.mkString(" "))
    }

    val feLanguage = languages.map(l => Try(NLPLanguage.withName(l.head.lang))) match {
      case Some(Success(language)) => language
      case _ => defaultLanguage
    }

    Try(FeatureExtractor(feLanguage, nlpResResolver)).recoverWith {
      case e =>
        //log.error(e, s"Cannot create feature extractor for language '$feLanguage' - defaulting to '$defaultLanguage'")
        Try(FeatureExtractor(defaultLanguage, nlpResResolver))
    }.map(_.extractFeatures(p, languages)).get
  }

}
