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
import com.socrata.soda2.consumer.Row

object SoQLQuery {
  def main(args: Array[String]) {
    val clientConfig = new AsyncHttpClientConfig.Builder().
      setSSLContext(SSLContext.getDefault). // Without this, ALL SSL certificates are treated as valid
      build()
    val client = new AsyncHttpClient(clientConfig)
    try {
      val service = new HttpConsumer(client, "data.cityofchicago.org")

      val future = service.query("ydr8-5enu", "select * limit 2").foreach {
        row => println(row.toString())
      }

      println("Waiting...")
      println(Await.result(future, Duration.Inf))
      println("Done.")
    } finally {
      client.close()
    }
  }
}
