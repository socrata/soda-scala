package com.socrata.soda2.publisher.http

import scala.concurrent.ExecutionContext

import com.ning.http.client.AsyncHttpClient

import com.socrata.soda2.consumer.http.{LowLevelHttp, HttpConsumer}
import com.socrata.soda2.publisher.Publisher
import com.socrata.http.{NoAuth, Authorization}
import com.socrata.future.ExecutionContextTimer

class HttpPublisher(lowLevel: LowLevelHttp) extends HttpConsumer(lowLevel) with Publisher {
  private[socrata] def this(client: AsyncHttpClient, logicalHost: String, physicalHost: String, port: Int = 443, secure: Boolean = true, authorization: Authorization = NoAuth)(implicit executionContext: ExecutionContext, timer: ExecutionContextTimer) =
    this(new LowLevelHttp(client, logicalHost, physicalHost, port, secure, authorization))

  def this(client: AsyncHttpClient, host: String, port: Int = 443, authorization: Authorization = NoAuth)(implicit executionContext: ExecutionContext, timer: ExecutionContextTimer) =
    this(client, host, host, port, true, authorization)
}
