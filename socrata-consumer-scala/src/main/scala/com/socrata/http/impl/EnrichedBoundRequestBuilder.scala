package com.socrata.http
package impl

import com.ning.http.client.AsyncHttpClient

import com.socrata.future.{ExecutionContext, Future}

class EnrichedBoundRequestBuilder(b: AsyncHttpClient#BoundRequestBuilder) {
  def makeRequest[T](consumer: StatusConsumer[T])(implicit executionContext: ExecutionContext): Future[T] = {
    WrappedFuture(b.execute(new NiceAsyncHandler(consumer)))
  }
}
