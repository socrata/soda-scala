package com.socrata.soda2.http

import com.socrata.soda2.SodaProtocolException

class IllegalResponseCharsetNameException(val charset: String, cause: Throwable)
  extends SodaProtocolException("Bad response charset name: " + charset, cause)
