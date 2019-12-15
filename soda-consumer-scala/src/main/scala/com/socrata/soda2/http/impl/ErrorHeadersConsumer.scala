package com.socrata.soda2.http
package impl

import com.rojoma.json.v3.ast.JObject
import com.rojoma.json.v3.util.{SimpleJsonCodecBuilder}
import com.rojoma.json.v3.codec.JsonDecode
import com.socrata.soda2.exceptions.soda1.Soda1Exception
import com.socrata.soda2.exceptions.ServerException
import com.socrata.http.{BodyConsumer, Headers, HeadersConsumer}
import com.socrata.soda2.{InvalidResponseJsonException, Resource}
import HeadersConsumerUtils._

private[http] class ErrorHeadersConsumer(resource: Resource, code: Int) extends HeadersConsumer[Nothing] {
  import ErrorHeadersConsumer._
  def apply(headers: Headers): Either[BodyConsumer[Nothing], Nothing] = {
    val codec = jsonCodec(headers)
    Left(new SingleJValueBodyConsumer(codec).map {
      case value: JObject =>
        if(headers.contains("x-error-code") || headers.contains("x-error-message")) processLegacyError(resource, code, value)
        else processError(value)
      case other =>
        throw new InvalidResponseJsonException(other, "Error response body was not an object")
    })
  }
}

private[http] object ErrorHeadersConsumer {
  case class Error(code: String, message: String, data: Option[JObject])
  implicit val errorCodec = SimpleJsonCodecBuilder[Error].build(
    "code", _.code,
    "message", _.message,
    "data", _.data)

  def processError(errorObject: JObject): Nothing = {
    JsonDecode.fromJValue[Error](errorObject) match {
      case Right(Error(code, message, data)) =>
        throw ServerException(code, message, data)
      case Left(_) =>
        badErrorBody(errorObject)
    }
  }

  case class LegacyError(code: Option[String], message: Option[String])
  implicit val legacyCodec = SimpleJsonCodecBuilder[LegacyError].build("code", _.code, "message", _.message)

  def processLegacyError(resource: Resource, status: Int, errorObject: JObject): Nothing = {
    JsonDecode.fromJValue[LegacyError](errorObject) match {
      case Right(LegacyError(code, message)) =>
        throw Soda1Exception(resource, status, code.getOrElse("internal error"), message)
      case Left(_) =>
        badErrorBody(errorObject)
    }
  }

  def badErrorBody(errorObject: JObject): Nothing =
    throw new InvalidResponseJsonException(errorObject, "Response body was not interpretable as an error")
}
