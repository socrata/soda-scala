package com.socrata.http
package impl

import scala.collection.JavaConverters._

import com.ning.http.client.{FluentStringsMap, Realm, RequestBuilderBase}
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
    }

  /** Replace any existing query parameters with the given ones. */
  def setQueryParametersS(params: Map[String, Seq[String]]) =
    b.setQueryParameters(new FluentStringsMap(params.mapValues(_.asJavaCollection).asJava))

  /** Replace any existing query parameters with the given ones, if not None, leaving them
   * alone if it is. */
  def maybeSetQueryParametersS(params: Option[Map[String, Seq[String]]]) =
    params.map(setQueryParametersS).getOrElse(b)
}
