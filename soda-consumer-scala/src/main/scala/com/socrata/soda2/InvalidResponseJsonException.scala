package com.socrata.soda2

import com.rojoma.json.ast.JValue

/** Thrown if a response is a valid JSON datum, but cannot be interpreted according to the
 * SODA2 specification. */
class InvalidResponseJsonException(val badDatum: JValue, message: String, cause: Throwable = null)
  extends SodaProtocolException(message, cause)
