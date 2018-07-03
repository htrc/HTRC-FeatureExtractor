package org.hathitrust.htrc.tools.featureextractor.features

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object VolumeFeatures {
  val schemaVersion: String = "4.0"

  def apply(pages: Seq[PageFeatures]): VolumeFeatures = {
    val dateTimeFormatter = DateTimeFormat forPattern "yyyy-MM-dd'T'HH:mm"
    val dateCreated = DateTime.now().toString(dateTimeFormatter)

    new VolumeFeatures(
      schemaVersion = schemaVersion,
      dateCreated = dateCreated,
      pageCount = pages.size,
      pages = pages
    )
  }
}

case class VolumeFeatures(schemaVersion: String,
                          dateCreated: String,
                          pageCount: Int,
                          pages: Seq[PageFeatures])