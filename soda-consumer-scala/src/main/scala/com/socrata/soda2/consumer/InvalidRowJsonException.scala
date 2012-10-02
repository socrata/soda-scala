package com.socrata.soda2.consumer

import com.rojoma.json.ast.JValue

import com.socrata.soda2.InvalidResponseJsonException

/** Thrown if a datum read out of the list of rows cannot be interpreted as a [[com.socrata.soda2.consumer.Row]]. */
class InvalidRowJsonException(badDatum: JValue, message: String, cause: Throwable = null)
  extends InvalidResponseJsonException(badDatum, message, cause)
