package com.socrata.consumer.http

import com.ning.http.client.AsyncHttpClient

import com.socrata.consumer.Simple
import com.socrata.http.{Authorization, NoAuth}

class SimpleHttp(lowLevel: LowLevelHttp) extends Simple(lowLevel) {
  def this(client: AsyncHttpClient, host: String, port: Int = 443, authorization: Authorization = NoAuth) =
    this(new LowLevelHttp(client, host, port, authorization))
}


