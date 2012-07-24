package com.socrata
package `consumer-impl`

import com.ning.http.client.{RequestBuilderBase, Realm}
import com.ning.http.client.Realm.RealmBuilder
import com.socrata.consumer.{Authorization, NoAuth, BasicAuth}

class EnrichedRequestBuilder[T <: RequestBuilderBase[T]](b: T) {
  def authorize(auth: Authorization): T =
    auth match {
      case NoAuth =>
        b
      case BasicAuth(username, pwd, appToken) =>
        b.setRealm(new RealmBuilder().setScheme(Realm.AuthScheme.BASIC).setPrincipal(username).setPassword(pwd).build).addHeader("X-App-Token", appToken)
    }
}
