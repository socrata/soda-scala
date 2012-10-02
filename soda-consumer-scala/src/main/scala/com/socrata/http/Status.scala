package com.socrata.http

/** A simple encapsulation of the status line from an HTTP response. */
case class Status(code: Int, text: String, protocol: String, majorVersion: Int, minorVersion: Int) {
  def isSuccess = 200 <= code && code <= 299
  def isRedirect = 300 <= code && code <= 399
  def isClientError = 400 <= code && code <= 499
  def isServerError = 500 <= code && code <= 599
}
