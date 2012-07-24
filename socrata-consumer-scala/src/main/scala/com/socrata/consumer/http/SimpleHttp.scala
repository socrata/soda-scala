package com.socrata
package consumer
package http

import com.ning.http.client.AsyncHttpClient

class SimpleHttp(lowLevel: LowLevelHttp) extends Simple(lowLevel) {
  def this(client: AsyncHttpClient, host: String, port: Int = 443, authorization: Authorization = NoAuth) =
    this(new LowLevelHttp(client, host, port, authorization))
}


