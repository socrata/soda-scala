package com.socrata.soda2.http
package impl

import scala.{collection => sc}
import scala.io.Codec

import java.nio.charset.{IllegalCharsetNameException, UnsupportedCharsetException, CodingErrorAction}
import javax.activation.MimeType

private[http] object HeadersConsumerUtils {
  def codecFor(charset: String): Codec =
    try {
      Codec(charset).onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE)
    } catch {
      case _: UnsupportedCharsetException =>
        throw UnsupportedResponseCharsetException(charset)
      case _: IllegalCharsetNameException =>
        throw IllegalResponseCharsetNameException(charset)
    }

  /**Checks that there is a content-type header, and that it is application/json, and returns the appropriate codec. */
  def jsonCodec(headers: sc.Map[String, Seq[String]]): Codec =
    headers.get("Content-Type").map(_.last) match {
      case Some(contentType) =>
        val mimeType = new MimeType(contentType)
        if (mimeType.getBaseType != "application/json") throw ResponseNotJSONException(Some(contentType))
        val charset = Option(mimeType.getParameter("charset")).getOrElse("iso-8859-1")
        codecFor(charset)
      case None =>
        throw ResponseNotJSONException(None)
    }
}
