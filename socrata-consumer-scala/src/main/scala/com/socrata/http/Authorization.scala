package com.socrata.http

sealed abstract class Authorization

case object NoAuth extends Authorization
case class BasicAuth(username: String, password: String, appToken: String) extends Authorization
