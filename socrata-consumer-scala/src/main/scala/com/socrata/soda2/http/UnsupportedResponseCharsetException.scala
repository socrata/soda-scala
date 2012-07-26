package com.socrata.soda2.http

import com.socrata.soda2.ProtocolException

class UnsupportedResponseCharsetException(val charset: String, cause: Throwable)
  extends ProtocolException("Unsupported response charset: " + charset, cause)
