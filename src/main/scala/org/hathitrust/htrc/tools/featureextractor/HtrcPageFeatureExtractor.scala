package org.hathitrust.htrc.tools.featureextractor

import org.hathitrust.htrc.data.HtrcStructuredPage
import org.hathitrust.htrc.tools.featureextractor.features.HtrcPageFeatures
import tdm.featureextractor.PageFeatureExtractor

object HtrcPageFeatureExtractor {

  def extractPageFeatures(page: HtrcStructuredPage): HtrcPageFeatures = {
    val features = PageFeatureExtractor.extractPageFeatures(page)

    HtrcPageFeatures(
      seq = page.seq,
      version = features.version,
      language = features.language,
      tokenCount =  features.tokenCount,
      lineCount = features.lineCount,
      emptyLineCount = features.emptyLineCount,
      sentenceCount = features.sentenceCount,
      header = features.header,
      body = features.body,
      footer = features.footer
    )
  }

}
