package com.socrata.http

import scala.{collection => sc}
import scala.io.Codec
import java.nio.charset.{UnsupportedCharsetException, CodingErrorAction}
import javax.activation.MimeType

object HeadersConsumerUtils {
  def codecFor(charset: String): Option[Codec] =
    try {
      Some(Codec(charset).onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE))
    } catch {
      case _: UnsupportedCharsetException => None
    }

  /** Checks that there is a content-type header, and that it is application/json, and returns the appropriate codec. */
  def jsonCodec(headers: sc.Map[String, Seq[String]]): Option[Codec] =
    headers.get("Content-Type").map(_.last) match {
      case Some(contentType) =>
        val mimeType = new MimeType(contentType)
        if(mimeType.getBaseType != "application/json") error("NYI") // FIXME: return none?
        val charset = Option(mimeType.getParameter("charset")).getOrElse("iso-8859-1")
        codecFor(charset)
      case None =>
        None
    }
}
