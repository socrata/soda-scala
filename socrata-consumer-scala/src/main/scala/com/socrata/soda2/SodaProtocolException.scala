package com.socrata.soda2

/** Thrown if a valid transport (i.e., HTTP) response was received but it was not legal
 * according to the SODA2 specification. This includes things like non-JSON or malformed JSON
 * responses, unknown character encodings, and other such errors which lie outside the
 * scope of SODA.  It does ''not'' include problems such as socket timeouts, SSL errors, or
 * broken HTTP messages. Nor does it include errors that are defined by the SODA2 spec,
 * which are subclasses of [[com.socrata.soda2.SodaException]].
 *
 * @see [[com.socrata.soda2.SodaException]] */
abstract class SodaProtocolException(message: String = null, cause: Throwable = null)
  extends RuntimeException(message, cause)
