package com.socrata.soda2.http

import com.socrata.soda2.ProtocolException

case class ResponseNotJSONException(contentType: Option[String]) extends ProtocolException(message = ResponseNotJSONException.formatMessage(contentType))

object ResponseNotJSONException {
  private def formatMessage(contentType: Option[String]): String = contentType match {
    case None => "No content-type received"
    case Some(ct) => "Received an illegal Content-Type: " + ct
  }
}

