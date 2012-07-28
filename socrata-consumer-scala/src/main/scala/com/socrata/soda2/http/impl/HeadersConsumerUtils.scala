package com.socrata.soda2.http
package impl

import scala.io.Codec

import java.nio.charset.{IllegalCharsetNameException, UnsupportedCharsetException, CodingErrorAction}
import javax.activation.MimeType

import com.socrata.http.Headers

private[http] object HeadersConsumerUtils {
  def codecFor(charset: String): Codec =
    try {
      Codec(charset).onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE)
    } catch {
      case e: UnsupportedCharsetException =>
        throw new UnsupportedResponseCharsetException(charset, e)
      case e: IllegalCharsetNameException =>
        throw new IllegalResponseCharsetNameException(charset, e)
    }

  /**Checks that there is a content-type header, and that it is application/json, and returns the appropriate codec. */
  def jsonCodec(headers: Headers): Codec =
    headers.get("Content-Type").map(_.last) match {
      case Some(contentType) =>
        val mimeType = new MimeType(contentType)
        if (mimeType.getBaseType != "application/json") throw new ResponseNotJSONException(Some(contentType))
        val charset = Option(mimeType.getParameter("charset")).getOrElse("iso-8859-1")
        codecFor(charset)
      case None =>
        throw new ResponseNotJSONException(None)
    }
}
