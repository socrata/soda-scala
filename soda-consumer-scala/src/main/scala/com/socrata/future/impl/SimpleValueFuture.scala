package com.socrata.future
package impl

private[future] class SimpleValueFuture[A](a: A) extends Future[A] {
  def onComplete[U](f: Either[Throwable, A] => U) {
    // FIXME: should this swallow exceptions?
    f(Right(a))
  }

  def result() = Some(a)

  def await() {}

  // This never launches anything into a background thread
  implicit def executionContext = ExecutionContext.noThreadsExecutionContext
}
