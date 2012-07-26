package com.socrata.soda2.http

import com.socrata.soda2.ProtocolException

case class UnsupportedResponseCharsetException(charset: String) extends ProtocolException(message = "Unsupported response charset: " + charset)
