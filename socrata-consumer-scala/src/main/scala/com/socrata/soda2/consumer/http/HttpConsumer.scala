package com.socrata.soda2.consumer.http

import com.ning.http.client.AsyncHttpClient

import com.socrata.soda2.consumer.Consumer
import com.socrata.future.ExecutionContext
import com.socrata.http.{Authorization, NoAuth}
import com.socrata.future.ExecutionContext

/** An implementation of [[com.socrata.soda2.consumer.Consumer]] which operates on a real HTTP server. */
class HttpConsumer(lowLevel: LowLevelHttp) extends Consumer(lowLevel) {
  /** Sets up the application's execution environment for making queries.
   *
   * @param client The [[com.ning.http.client.AsyncHttpClient]] to use for making queries.
   * @param host The hostname to which to connect.
   * @param port The port on which the SODA2 server is listening for HTTPS traffic.
   * @param authorization The authorization strategy to use for making queries with this object.
   * @param executionContext A strategy for starting asynchronous tasks.
   */
  def this(client: AsyncHttpClient, host: String, port: Int = 443, authorization: Authorization = NoAuth)(implicit executionContext: ExecutionContext) =
    this(new LowLevelHttp(client, host, port, authorization))
}


