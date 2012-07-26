package com.socrata.http
package impl

import java.util.concurrent.ExecutionException

import com.ning.http.client.ListenableFuture

import com.socrata.future.{ExecutionContext, Promise, Future}

private[http] object WrappedFuture {
  def apply[A](underlying: ListenableFuture[A])(implicit executionContext: ExecutionContext): Future[A] = {
    val promise = new Promise[A]
    underlying.addListener(new Runnable {
      override def run() {
        try {
          promise.fulfill(underlying.get())
        } catch {
          case e: ExecutionException =>
            promise.break(e.getCause)
          case e: Throwable =>
            promise.break(e)
        }
      }
    }, executionContext.asJava)
    promise.future
  }
}
