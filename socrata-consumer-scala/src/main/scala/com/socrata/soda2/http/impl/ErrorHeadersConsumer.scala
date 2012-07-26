package com.socrata.soda2.http
package impl

import scala.{collection => sc}

import com.rojoma.json.ast.{JObject, JString, JValue}

import com.socrata.http.{BodyConsumer, HeadersConsumer}

import HeadersConsumerUtils._

private[http] object ErrorHeadersConsumer extends HeadersConsumer[Nothing] {
  def apply(headers: sc.Map[String, Seq[String]]): Either[BodyConsumer[Nothing], Nothing] = {
    val codec = jsonCodec(headers)
    Left(new SingleJValueBodyConsumer(codec).map(processError))
  }

  def processError(errorObject: JValue): Nothing = {
    error("NYI")
  }
}
