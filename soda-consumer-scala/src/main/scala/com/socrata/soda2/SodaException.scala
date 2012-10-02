package com.socrata.soda2

/** The superclass of all exceptions caused by conditions defined by the SODA2 specification.
 * @see [[com.socrata.soda2.SodaProtocolException]] */
abstract class SodaException(message: String = null, cause: Throwable = null)
  extends RuntimeException(message, cause)
