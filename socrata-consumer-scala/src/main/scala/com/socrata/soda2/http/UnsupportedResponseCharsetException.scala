package com.socrata.soda2.http

import com.socrata.soda2.SodaProtocolException

class UnsupportedResponseCharsetException(val charset: String, cause: Throwable)
  extends SodaProtocolException("Unsupported response charset: " + charset, cause)
