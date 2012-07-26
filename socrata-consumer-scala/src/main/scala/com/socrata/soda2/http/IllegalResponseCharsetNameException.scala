package com.socrata.soda2.http

import com.socrata.soda2.ProtocolException

case class IllegalResponseCharsetNameException(charset: String) extends ProtocolException(message = "Bad response charset name: " + charset)
