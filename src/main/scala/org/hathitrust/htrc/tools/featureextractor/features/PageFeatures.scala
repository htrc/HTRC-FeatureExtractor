package org.hathitrust.htrc.tools.featureextractor.features

object PageFeatures {
  def apply(seq: String,
            version: String,
            language: Option[String],
            header: Option[SectionFeatures],
            body: Option[SectionFeatures],
            footer: Option[SectionFeatures]): PageFeatures = {
    val allSections = List(header, body, footer)
    val nonEmptySections = allSections.collect { case Some(s) => s }

    val tokenCount = nonEmptySections.foldLeft(0)(_ + _.tokenCount)
    val lineCount = nonEmptySections.foldLeft(0)(_ + _.lineCount)
    val emptyLineCount = nonEmptySections.foldLeft(0)(_ + _.emptyLineCount)
    val sentenceCount =
      if (nonEmptySections.forall(_.sentenceCount.isDefined))
        Some(nonEmptySections.foldLeft(0)(_ + _.sentenceCount.get))
      else None

    new PageFeatures(
      seq = seq,
      version = version,
      language = language,
      tokenCount = tokenCount,
      lineCount = lineCount,
      emptyLineCount = emptyLineCount,
      sentenceCount = sentenceCount,
      header = header,
      body = body,
      footer = footer
    )
  }
}

/**
  * Object recording aggregate features at the page level
  *
  * @param seq            The page sequence id
  * @param tokenCount     The total token count for the page
  * @param lineCount      The total line count for the page
  * @param emptyLineCount The empty line count for the page
  * @param sentenceCount  The sentence count for the page
  * @param header         The page header features
  * @param body           The page body features
  * @param footer         The page footer features
  * @param language       The identified page language (if any)
  */
case class PageFeatures(seq: String,
                        version: String,
                        language: Option[String],
                        tokenCount: Int,
                        lineCount: Int,
                        emptyLineCount: Int,
                        sentenceCount: Option[Int],
                        header: Option[SectionFeatures],
                        body: Option[SectionFeatures],
                        footer: Option[SectionFeatures]) extends BasicFeatures