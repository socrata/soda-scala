package com.socrata.soda2.http

import com.socrata.soda2.SodaProtocolException
import com.socrata.http.Status

/** Thrown if an HTTP response code that is not used by SODA is received. */
class InvalidHttpStatusException(val status: Status) extends SodaProtocolException("Unknown HTTP status: " + status.code + " " + status.text)
