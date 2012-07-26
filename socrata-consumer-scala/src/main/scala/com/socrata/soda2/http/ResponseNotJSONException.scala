package com.socrata.soda2.http

import com.socrata.soda2.SodaProtocolException

class ResponseNotJSONException(val contentType: Option[String])
  extends SodaProtocolException(ResponseNotJSONException.formatMessage(contentType))

object ResponseNotJSONException {
  private def formatMessage(contentType: Option[String]): String = contentType match {
    case None => "No content-type received"
    case Some(ct) => "Received an illegal Content-Type: " + ct
  }
}

