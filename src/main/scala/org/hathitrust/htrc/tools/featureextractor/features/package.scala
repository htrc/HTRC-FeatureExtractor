package org.hathitrust.htrc.tools.featureextractor

import play.api.libs.json._
import play.api.libs.functional.syntax._
import tdm.featureextractor.features.SectionFeatures

package object features {

  // Need to define explicit Writes to force the generation of 'null' values for Option[T] fields.
  // The default Json.writes[T] macro omits the generation of attributes when their value is 'None'.

  implicit val sectionFeaturesWrites: OWrites[SectionFeatures] = (
    (__ \ 'tokenCount).write[Int] and
    (__ \ 'lineCount).write[Int] and
    (__ \ 'emptyLineCount).write[Int] and
    (__ \ 'sentenceCount).write[Option[Int]] and
    (__ \ 'capAlphaSeq).write[Int] and
    (__ \ 'beginCharCount).write[Map[String, Int]] and
    (__ \ 'endCharCount).write[Map[String, Int]] and
    (__ \ 'tokenPosCount).write[Map[String, Map[String, Int]]]
  )(unlift(SectionFeatures.unapply))

  implicit val pageFeaturesWrites: OWrites[HtrcPageFeatures] = (
    (__ \ 'seq).write[String] and
    (__ \ 'version).write[String] and
    (__ \ 'language).write[Option[String]] and
    (__ \ 'tokenCount).write[Int] and
    (__ \ 'lineCount).write[Int] and
    (__ \ 'emptyLineCount).write[Int] and
    (__ \ 'sentenceCount).write[Option[Int]] and
    (__ \ 'header).write[Option[SectionFeatures]] and
    (__ \ 'body).write[Option[SectionFeatures]] and
    (__ \ 'footer).write[Option[SectionFeatures]]
  )(unlift(HtrcPageFeatures.unapply))

  implicit val volumeFeaturesWrites: OWrites[VolumeFeatures] = Json.writes[VolumeFeatures]
  implicit val efWrites: OWrites[EF] = Json.writes[EF]

}
