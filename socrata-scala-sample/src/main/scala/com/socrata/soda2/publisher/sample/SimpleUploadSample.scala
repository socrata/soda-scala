package com.socrata.soda2.publisher.sample

import javax.net.ssl.SSLContext

import com.ning.http.client.{AsyncHttpClientConfig, AsyncHttpClient}

import com.socrata.future.ExecutionContext.implicits._
import com.socrata.soda2.publisher.http.HttpPublisher
import com.socrata.http.BasicAuth
import com.socrata.soda2.Resource
import com.socrata.soda2.values.{SodaNumber, SodaString}

object SimpleUploadSample {
  def main(args: Array[String]) {
    val clientConfig = new AsyncHttpClientConfig.Builder().
      setSSLContext(SSLContext.getDefault). // Without this, ALL SSL certificates are treated as valid
      build()
    val client = new AsyncHttpClient(clientConfig)

    try {
      // to run this example, you need a Socrata account from
      // opendata.socrata.com (or any other Socrata-powered data
      // site), a dataset on opendata, and an app token on opendata.
      //
      // The dataset should have (at least) two columns, one named
      // "text" with type text and one named "number" with type
      // number.
      val service = new HttpPublisher(client, "opendata.socrata.com", authorization =
        BasicAuth("my.email@example.com", "my password", "my app token"))

      // The id is either the nine-character "xxxx-xxxx" string that
      // appears in your dataset's URL, or the "resource name" that
      // you can assign to it on the dataset's metadata editing page.
      val myResource = Resource("my dataset's id")

      // This makes a working copy (unnecessary with SODA2, but it still does it for sample purposes)
      // and does a couple of inserts (one batch-insert of a single row, and, one single-insert)
      val future = for {
        r <- service.makeWorkingCopy(myResource)
        updateResult <- service.upsert(r, additions = Seq(Map("text" -> SodaString("Hello!"), "number" -> SodaNumber(33.3))))
        updateResult2 <- service.upsertSingle(r, Map("text" -> SodaString("Goodbye!"), "number" -> SodaNumber(-15)))
        _ <- service.publish(r)
      } yield (updateResult, updateResult2)

      // wait for it to get done and print the values that were
      // returned -- a batch summary and the upserted row,
      // respectively.  This throws an exception if something went
      // wrong (a socket error, or a timeout, or a SODA error)
      println(future())
    } finally {
      client.close()
    }
  }
}
