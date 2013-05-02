package com.socrata.http
package impl

import com.ning.http.client.{Response, AsyncHttpClient}
import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.Executor

class EnrichedBoundRequestBuilder(b: AsyncHttpClient#BoundRequestBuilder) {
  /** Build the request and send it off to an HTTP server.
   *
   * @param consumer The entry point into the state machine managed by a [[com.socrata.http.NiceAsyncHandler]]
   * @param executionContext A strategy for starting tasks asynchronously. */
  def makeRequest[T](consumer: StatusConsumer[T])(implicit executionContext: ExecutionContext): Future[T] = {
    WrappedFuture(b.execute(new NiceAsyncHandler(consumer)))
  }

  def makeRequest()(implicit executionContext: ExecutionContext): Future[Response] = {
    WrappedFuture(b.execute())
  }
}
