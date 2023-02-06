package org.hathitrust.htrc.tools.featureextractor

import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.hathitrust.htrc.featureextractor.features.SectionFeatures

package object features {

  // Need to define explicit Writes to force the generation of 'null' values for Option[T] fields.
  // The default Json.writes[T] macro omits the generation of attributes when their value is 'None'.

  implicit val sectionFeaturesWrites: OWrites[SectionFeatures] = (
    (__ \ Symbol("tokenCount")).write[Int] and
    (__ \ Symbol("lineCount")).write[Int] and
    (__ \ Symbol("emptyLineCount")).write[Int] and
    (__ \ Symbol("sentenceCount")).write[Option[Int]] and
    (__ \ Symbol("capAlphaSeq")).write[Int] and
    (__ \ Symbol("beginCharCount")).write[Map[String, Int]] and
    (__ \ Symbol("endCharCount")).write[Map[String, Int]] and
    (__ \ Symbol("tokenPosCount")).write[Map[String, Map[String, Int]]]
  )(unlift(SectionFeatures.unapply))

  implicit val pageFeaturesWrites: OWrites[HtrcPageFeatures] = (
    (__ \ Symbol("seq")).write[String] and
    (__ \ Symbol("version")).write[String] and
    (__ \ Symbol("language")).write[Option[String]] and
    (__ \ Symbol("tokenCount")).write[Int] and
    (__ \ Symbol("lineCount")).write[Int] and
    (__ \ Symbol("emptyLineCount")).write[Int] and
    (__ \ Symbol("sentenceCount")).write[Option[Int]] and
    (__ \ Symbol("header")).write[Option[SectionFeatures]] and
    (__ \ Symbol("body")).write[Option[SectionFeatures]] and
    (__ \ Symbol("footer")).write[Option[SectionFeatures]]
  )(unlift(HtrcPageFeatures.unapply))

  implicit val volumeFeaturesWrites: OWrites[VolumeFeatures] = Json.writes[VolumeFeatures]
  implicit val efWrites: OWrites[EF] = Json.writes[EF]

}
