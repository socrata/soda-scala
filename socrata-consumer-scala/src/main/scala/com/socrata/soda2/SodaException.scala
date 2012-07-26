package com.socrata.soda2

/** The base class of all exceptions thrown by the SODA2 API. */
abstract class SodaException(message: String = null, cause: Throwable = null) extends RuntimeException(message, cause)
