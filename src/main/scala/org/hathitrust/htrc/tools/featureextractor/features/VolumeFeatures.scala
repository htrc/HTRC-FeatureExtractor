package org.hathitrust.htrc.tools.featureextractor.features

import java.util.Objects

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsObject, Json}

object VolumeFeatures {
  private val `type`: String = "DataFeedItem"
  private val handleUrlBase: String = "http://hdl.handle.net/2027/"
  private val schemaVersion: String = "https://schemas.hathitrust.org/EF_Schema_FeaturesSubSchema_v_3.0"

  def apply(volId: String, pages: Seq[HtrcPageFeatures]): VolumeFeatures = {
    require(Objects.nonNull(volId), "volId cannot be null")
    require(Objects.nonNull(pages), "pages cannot be null")

    val dateTimeFormatter = DateTimeFormat forPattern "yyyyMMdd"
    val dateCreated = DateTime.now().toString(dateTimeFormatter).toInt

    new VolumeFeatures(
      `type` = `type`,
      id = handleUrlBase + volId,
      schemaVersion = schemaVersion,
      dateCreated = dateCreated,
      pageCount = pages.size,
      pages = pages
    )
  }
}

case class VolumeFeatures(`type`: String,
                          id: String,
                          schemaVersion: String,
                          dateCreated: Int,
                          pageCount: Int,
                          pages: Seq[HtrcPageFeatures])