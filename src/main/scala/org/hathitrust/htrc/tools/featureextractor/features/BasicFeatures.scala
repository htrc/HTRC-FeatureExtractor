package org.hathitrust.htrc.tools.featureextractor.features

/**
  * Trait representing the basic features that will be recorded for a page
  */
trait BasicFeatures extends Serializable {
  val tokenCount: Int
  val lineCount: Int
  val emptyLineCount: Int
  val sentenceCount: Option[Int]
}