package com.socrata
package `consumer-impl`

import http.StatusConsumer
import future.{WrappedFuture, ExecutionContext, Future}
import com.ning.http.client.AsyncHttpClient

class EnrichedBoundRequestBuilder(b: AsyncHttpClient#BoundRequestBuilder) {
  def makeRequest[T](consumer: StatusConsumer[T])(implicit executionContext: ExecutionContext): Future[T] = {
    WrappedFuture(b.execute(new NiceAsyncHandler(consumer)))
  }
}
