package com.socrata.soda2

/** A message was received that was not legal according to the SODA2 specification. */
abstract class ProtocolException(message: String = null, cause: Throwable = null) extends SodaException(message, cause)
