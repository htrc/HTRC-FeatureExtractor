package org.hathitrust.htrc.tools.featureextractor

case class HTRCPairtreeDocumentException(msg: String, cause: Throwable) extends Exception(msg, cause)