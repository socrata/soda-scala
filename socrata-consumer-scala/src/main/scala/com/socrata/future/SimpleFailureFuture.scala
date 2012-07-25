package com.socrata.future

class SimpleFailureFuture(e: Throwable) extends Future[Nothing] {
  def onComplete[U](f: Either[Throwable, Nothing] => U) { f(Left(e)) } // FIXME: should this swallow exceptions?
  def result() = throw e
  def await() {}

  override def flatMap[B](f: Nothing => Future[B]): Future[B] = this
  override def map[B](f: Nothing => B): Future[B] = this
}
