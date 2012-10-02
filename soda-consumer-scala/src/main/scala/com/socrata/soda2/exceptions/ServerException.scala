package com.socrata.soda2.exceptions

import com.rojoma.json.ast.JObject

import com.socrata.soda2.SodaException

abstract class ServerException(message: String) extends SodaException(message)

class UnknownServerException(val code: String, message: String, val data: Option[JObject]) extends ServerException("Unknown server exception: " + code + ": " + message)

object ServerException {
  def apply(code: String, message: String, data: Option[JObject]): ServerException = code match {
    case other => throw new UnknownServerException(code, message, data)
  }
}
