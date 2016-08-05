package org.hathitrust.htrc.tools.featureextractor

import com.cybozu.labs.langdetect.Language

object JsonStats {
  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val stringCountWrites: Writes[Map[String, Int]] =
    (stringCounts: Map[String, Int]) => JsObject(stringCounts.map {
      case (s, count) => s -> JsNumber(count)
    }.toSeq)

  implicit val tokenPosCountWrites: Writes[Map[String, Map[String, Int]]] =
    (tokenPosCounts: Map[String, Map[String, Int]]) => JsObject(tokenPosCounts.map {
      case (token, posCounts) => token -> Json.toJson(posCounts)
    }.toSeq)

  implicit val languageWrites: Writes[Language] = (language: Language) => Json.obj(
    language.lang -> language.prob.formatted("%.2f")
  )

  implicit val sectionStatsWrites: Writes[SectionStats] = Json.writes[SectionStats]

  implicit val pageStatsWrites: Writes[PageStats] = (
      (JsPath \ "seq").write[String] and
      (JsPath \ "tokenCount").write[Int] and
      (JsPath \ "lineCount").write[Int] and
      (JsPath \ "emptyLineCount").write[Int] and
      (JsPath \ "sentenceCount").write[Int] and
      (JsPath \ "header").write[SectionStats] and
      (JsPath \ "body").write[SectionStats] and
      (JsPath \ "footer").write[SectionStats] and
      (JsPath \ "languages").write[Seq[Language]]
    )(unlift(PageStats.unapply))
}