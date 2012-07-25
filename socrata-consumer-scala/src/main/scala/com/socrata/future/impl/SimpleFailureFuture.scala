package com.socrata.future
package impl

private[future] class SimpleFailureFuture(e: Throwable) extends Future[Nothing] {
  def onComplete[U](f: Either[Throwable, Nothing] => U) {
    // FIXME: should this swallow exceptions?
    f(Left(e))
  }

  def result() = throw e

  def await() {}

  override def flatMap[B](f: Nothing => Future[B]): Future[B] = this

  override def map[B](f: Nothing => B): Future[B] = this
}
