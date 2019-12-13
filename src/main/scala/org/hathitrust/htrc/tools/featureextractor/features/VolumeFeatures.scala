package org.hathitrust.htrc.tools.featureextractor.features

import java.util.Objects

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsObject, Json}

object VolumeFeatures {
  private val `type`: String = "DataFeedItem"
  private val handleUrlBase: String = "http://hdl.handle.net/2027/"
  private val schemaVersion: String = "https://schemas.hathitrust.org/EF_Schema_FeaturesSubSchema_v_3.0"
  private val creator: JsObject = Json.obj(
    "id" -> "https://analytics.hathitrust.org",
    "type" -> "Organization",
    "name" -> "HathiTrust Research Center"
  )

  def apply(volId: String, pages: Seq[HtrcPageFeatures]): VolumeFeatures = {
    require(Objects.nonNull(volId), "volId cannot be null")
    require(Objects.nonNull(pages), "pages cannot be null")

    val dateTimeFormatter = DateTimeFormat forPattern "yyyy-MM-dd'T'HH:mm"
    val dateCreated = DateTime.now().toString(dateTimeFormatter)

    new VolumeFeatures(
      `type` = `type`,
      id = handleUrlBase + volId,
      schemaVersion = schemaVersion,
      dateCreated = dateCreated,
      creator = creator,
      pageCount = pages.size,
      pages = pages
    )
  }
}

case class VolumeFeatures(`type`: String,
                          id: String,
                          schemaVersion: String,
                          dateCreated: String,
                          creator: JsObject,
                          pageCount: Int,
                          pages: Seq[HtrcPageFeatures])