package org.hathitrust.htrc.tools.featureextractor

import com.cybozu.labs.langdetect.{DetectorFactory, Language}
import edu.illinois.i3.scala.nlp.NLPToolsFactory.NLPResourceResolver
import edu.illinois.i3.scala.nlp.{Language => NLPLanguage}
import edu.illinois.i3.scala.text.docstructure.{Page, PageWithStructure}
import org.apache.log4j.Logger

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

object Executor {

  @transient lazy val logger = Logger.getLogger("FeatureExtractor")

  @transient var initialized = false
  @transient val lock = new Object()

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

  def detectLanguages(page: Page with PageWithStructure): Option[Seq[Language]] = Try {
    val languageDetector = DetectorFactory.create()
    val text = page.getBody.map(_.text).mkString("\n")
    languageDetector.append(text)
    languageDetector.getProbabilities
  } match {
    case Success(langProbs) => Some(langProbs.toSeq)
    case _ => None
  }

  def computePageStats(p: Page with PageWithStructure,
                       nlpResResolver: NLPResourceResolver): PageStats = {
    val languages = detectLanguages(p) match {
      case l @ Some(langs) if langs.length > 1 =>
        // make sure the language detection is accurate - check that there are sufficient words
        // of >3 characters as context for the language detector, otherwise the results are
        // unreliable
        val wordsOnPage = p.getBody.map(_.text.trim).withFilter(_.nonEmpty)
          .flatMap(_.split( """\s+""").map(_.replaceAll( """[\.,!?;:'")]+$""", "")))
          .filter(_.forall(_.isLetter))
        if (wordsOnPage.count(_.length > 3) < 10) None else l
      case any => any
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
