package com.socrata.soda2.consumer.sample

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.Await

import javax.net.ssl.SSLContext
import java.util.concurrent.Executors

import com.ning.http.client.{AsyncHttpClientConfig, AsyncHttpClient}

import com.socrata.soda2.consumer.http.HttpConsumer
import com.socrata.soda2.values.SodaString
import com.socrata.future.ExecutionContextTimer.Implicits._

object SimpleQuery {
  def main(args: Array[String]) {
    val clientConfig = new AsyncHttpClientConfig.Builder().
      setSSLContext(SSLContext.getDefault). // Without this, ALL SSL certificates are treated as valid
      build()
    val client = new AsyncHttpClient(clientConfig)
    try {
      val service = new HttpConsumer(client, "explore.data.gov")

      // "select distinct(firstname) where lastname = 'clinton'" but
      // soda2 does not (yet) support "distinct".
      val future = service.query("644b-gaut", "namelast" -> "clinton").foldLeft(Set.empty[String]) { (firstNames, row) =>
        row("namefirst") match {
          case Some(SodaString(firstName)) => firstNames + firstName
          case _ => firstNames
        }
      }

      println("Waiting...")
      println(Await.result(future, Duration.Inf))
      println("Done.")
    } finally {
      client.close()
    }
  }
}
