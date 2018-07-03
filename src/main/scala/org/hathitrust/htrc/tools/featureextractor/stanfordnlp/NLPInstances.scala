package org.hathitrust.htrc.tools.featureextractor.stanfordnlp

import java.util.{Locale, Properties, concurrent}

import edu.stanford.nlp.pipeline.StanfordCoreNLP
import org.hathitrust.htrc.tools.featureextractor.Helper._
import org.hathitrust.htrc.tools.featureextractor.Main

import scala.util.{Failure, Success}

object NLPInstances {
  private val instances = new concurrent.ConcurrentHashMap[Locale, StanfordCoreNLP]()

  private def createInstance(locale: Locale): StanfordCoreNLP = {
    val lang = locale.getLanguage
    val langProps = s"/nlp/config/$lang.properties"
    logger.info(s"Loading ${locale.getDisplayLanguage} settings from $langProps")
    val props = loadPropertiesFromClasspath(langProps) match {
      case Success(p) => p
      case Failure(e) =>
        logger.error(s"Unable to load $lang settings", e)
        throw e
    }

    new StanfordCoreNLP(props)
  }

  val whitespaceTokenizer: StanfordCoreNLP = {
    val props = new Properties()
    props.put("annotators", "tokenize")
    props.put("tokenize.language", "Whitespace")
    new StanfordCoreNLP(props)
  }

  def forLanguage(lang: String): Option[StanfordCoreNLP] = forLocale(Locale.forLanguageTag(lang))

  def forLocale(locale: Locale): Option[StanfordCoreNLP] = {
    if (Main.supportedLanguages.contains(locale.getLanguage))
      Option(instances.get(locale)).orElse {
        this.synchronized {
          Option(instances.get(locale)).orElse {
            val instance = createInstance(locale)
            instances.putIfAbsent(locale, instance)
            Some(instance)
          }
        }
      }
    else None
  }
}