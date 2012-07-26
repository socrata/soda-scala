package com.socrata.soda2.http

import com.socrata.soda2.ProtocolException

class IllegalResponseCharsetNameException(val charset: String, cause: Throwable)
  extends ProtocolException("Bad response charset name: " + charset, cause)
