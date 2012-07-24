package com.socrata
package consumer
package sample

import com.ning.http.client.{AsyncHttpClientConfig, AsyncHttpClient}

import com.rojoma.json.ast.JValue
import javax.net.ssl.SSLContext

object SimpleQuery {
  def main(args: Array[String]) {
    val clientConfig = new AsyncHttpClientConfig.Builder().
      setSSLContext(SSLContext.getDefault). // Without this, ALL SSL certificates are treated as valid
      build()
    val client = new AsyncHttpClient(clientConfig)
    try {
      val service = new http.SimpleHttp(client, "explore.data.gov")

      val future = service.query(Resource("644b-gaut"), Map("namelast" -> "CLINTON")).foldLeft(Set.empty[JValue]) { (firstNames, row) =>
        firstNames + row("namefirst")
      }

      println("Waiting...")
      println(future())
      println("Done.")
    } finally {
      client.close()
    }
  }
}
