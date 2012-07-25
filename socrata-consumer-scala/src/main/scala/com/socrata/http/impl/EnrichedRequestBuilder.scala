package com.socrata.http.impl

import scala.collection.JavaConverters._

import com.ning.http.client.{FluentStringsMap, Realm, RequestBuilderBase}
import com.ning.http.client.Realm.RealmBuilder

import com.socrata.http.{Authorization, NoAuth, BasicAuth}

class EnrichedRequestBuilder[T <: RequestBuilderBase[T]](b: T) {
  def authorize(auth: Authorization): T =
    auth match {
      case NoAuth =>
        b
      case BasicAuth(username, pwd, appToken) =>
        b.setRealm(new RealmBuilder().setScheme(Realm.AuthScheme.BASIC).setPrincipal(username).setPassword(pwd).build).addHeader("X-App-Token", appToken)
    }

  def setQueryParametersS(params: Map[String, Seq[String]]) =
    b.setQueryParameters(new FluentStringsMap(params.mapValues(_.asJavaCollection).asJava))
}
