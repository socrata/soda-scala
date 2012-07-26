package com.socrata.soda2.http
package impl

import scala.{collection => sc}

import com.rojoma.json.ast.{JObject, JString, JValue}

import com.socrata.http.{BodyConsumer, HeadersConsumer}

import HeadersConsumerUtils._

private[http] object ErrorHeadersConsumer extends HeadersConsumer[Nothing] {
  def apply(headers: sc.Map[String, Seq[String]]): Either[BodyConsumer[Nothing], Nothing] =
    jsonCodec(headers) match {
      case Some(codec) =>
        Left(new SingleJValueBodyConsumer(codec).map(processError))
      case None =>
        error("NYI") // protocol error: wasn't application/json
    }

  def processError(errorObject: JValue): Nothing = {
    throw new SodaHttpException(errorObject.asInstanceOf[JObject]("message").asInstanceOf[JString].string)
  }
}
