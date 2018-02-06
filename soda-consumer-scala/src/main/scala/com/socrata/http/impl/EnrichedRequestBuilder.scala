package com.socrata.http
package impl

import scala.collection.JavaConverters._
import com.ning.http.client.{Cookie, FluentStringsMap, Realm, RequestBuilderBase}
import com.ning.http.client.Realm.RealmBuilder

class EnrichedRequestBuilder[T <: RequestBuilderBase[T]](b: T) {
  /** Set headers appropriate for the given [[com.socrata.http.Authorization]]. */
  def authorize(auth: Authorization): T =
    auth match {
      case NoAuth =>
        b
      case BasicAuth(username, pwd, appToken) =>
        val realm = new RealmBuilder().
          setScheme(Realm.AuthScheme.BASIC).
          setPrincipal(username).
          setPassword(pwd).
          setUsePreemptiveAuth(true).
          build()
        b.setRealm(realm).addHeader("X-App-Token", appToken)
      case CookieAuth(cookie) =>
        b.addCookie(new Cookie(null, "_core_session_id", cookie, null, -1, false))
    }

  /** Set request ID header if given */
  def maybeSetRequestId(requestId: Option[String]): T =
    requestId.fold(b)(b.setHeader("X-Socrata-RequestId", _))

  /** Replace any existing query parameters with the given ones. */
  def setQueryParametersS(params: Map[String, Seq[String]]) =
    b.setQueryParameters(new FluentStringsMap(params.mapValues(_.asJavaCollection).asJava))

  /** Augment any existing query parameters with the given ones. */
  def addQueryParameters(params: Map[String, Seq[String]]) = {
    for {
      (k, vs) <- params
      v <- vs
    } b.addQueryParameter(k, v)
    b
  }

  /** Replace any existing query parameters with the given ones, if not None, leaving them
   * alone if it is. */
  def maybeSetQueryParametersS(params: Option[Map[String, Seq[String]]]) =
    params.map(setQueryParametersS).getOrElse(b)

  /** Replace any existing form parameters with the given ones. */
  def setParametersS(params: Map[String, Seq[String]]) =
    b.setParameters(new FluentStringsMap(params.mapValues(_.asJavaCollection).asJava))

  /** Augment any existing form parameters with the given ones. */
  def addParameters(params: Map[String, Seq[String]]) = {
    for {
      (k, vs) <- params
      v <- vs
    } b.addParameter(k, v)
    b
  }

  /** Replace any existing form parameters with the given ones, if not None, leaving them
   * alone if it is. */
  def maybeSetParametersS(params: Option[Map[String, Seq[String]]]) =
    params.map(setParametersS).getOrElse(b)
}
