package com.socrata.soda2

/** Thrown if malformed JSON is returned from the SODA server. */
class MalformedResponseJsonException(message: String, cause: Throwable = null)
  extends SodaProtocolException(message, cause)
