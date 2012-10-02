package com.socrata.soda2.http

import com.rojoma.json.ast.JObject

/** High-level instructions for handling making multiple requests to produce the
 * result from a long-running server-side process. */
sealed abstract class NewRequest {
  /** The number of seconds the client should wait before issuing its new request. */
  def retryAfter: Int

  /** A [[com.rojoma.json.ast.JObject]] representing progress or status information.
   * Most responses will contain at least the field "message". */
  def details: JObject
}

/** The client should re-make the same request */
case class Retry(retryAfter: Int, details: JObject) extends NewRequest

/** The client should issue a GET to the same URL it made this request, but with
 * the given ticket in the query parameters. */
case class RetryWithTicket(ticket: String, retryAfter: Int, details: JObject) extends NewRequest

/** The client should issue a GET to a completely new URL */
case class Redirect(newUrl: String, retryAfter: Int,  details: JObject) extends NewRequest
