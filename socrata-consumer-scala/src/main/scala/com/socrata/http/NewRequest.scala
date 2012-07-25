package com.socrata.http

import com.rojoma.json.ast.JObject

sealed abstract class NewRequest {
  def retryAfter: Int
  def details: JObject
}
case class Retry(retryAfter: Int, details: JObject) extends NewRequest
case class Redirect(newUrl: String, retryAfter: Int,  details: JObject) extends NewRequest
case class RetryWithTicket(ticket: String, retryAfter: Int, details: JObject) extends NewRequest
