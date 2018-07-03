package org.hathitrust.htrc.tools.featureextractor

import java.util.Locale

import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.LanguageProfileReader
import com.optimaize.langdetect.text.CommonTextObjectFactories
import org.hathitrust.htrc.tools.featureextractor.Helper._

import scala.collection.JavaConverters._

object LanguageDetector {
  // use all language profiles except Korean (which often gets confused for Chinese) so that
  // Chinese detection is improved; HathiTrust has very few Korean volumes, and we don't have
  // a Korean tokenizer anyway
  private val languageProfiles =
    new LanguageProfileReader()
      .readAllBuiltIn()
      .asScala
      .filter(_.getLocale.getLanguage != "ko")
      .asJava

  private val languageDetector =
    LanguageDetectorBuilder
      .create(NgramExtractors.standard())
      .withProfiles(languageProfiles)
      .build()

  private val minConfidence = 0.75d

  def detectLanguage(text: String): Option[Locale] = {
    val textNoZeroWidthSpace = text.replaceAll("""\p{Cf}""", "")
    val textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText()
    val langProbs = languageDetector.getProbabilities(textObjectFactory.forText(textNoZeroWidthSpace)).asScala
    langProbs.headOption.filter(_.getProbability >= minConfidence).map(_.getLocale.toLocale)
  }
}
