package org.hathitrust.htrc.tools.featureextractor.features

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsObject, Json}

object EF {
  private val `@context`: String = "https://worksets.htrc.illinois.edu/context/ef_context.jsonld"
  private val schemaVersion: String = "https://schemas.hathitrust.org/EF_Schema_v_3.0"
  private val `type`: String = "DataFeed"
  private val publisher: JsObject = Json.obj(
    "id" -> "https://analytics.hathitrust.org",
    "type" -> "Organization",
    "name" -> "HathiTrust Research Center"
  )
  private val baseIdFormat: String = "https://data.analytics.hathitrust.org/extracted-features/%s/%s"

  def apply(htid: String, features: VolumeFeatures): EF = {
    val dateTimeFormatter = DateTimeFormat forPattern "yyyyMMdd"
    val datePublished = DateTime.now().toString(dateTimeFormatter).toInt

    new EF(
      `@context` = `@context`,
      schemaVersion = schemaVersion,
      id = baseIdFormat.format(datePublished, htid),
      htid = htid,
      `type` = `type`,
      publisher = publisher,
      datePublished = datePublished,
      features = features
    )
  }
}

case class EF(`@context`: String,
              schemaVersion: String,
              id: String,
              htid: String,
              `type`: String,
              publisher: JsObject,
              datePublished: Int,
              features: VolumeFeatures)