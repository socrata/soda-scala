package com.socrata.future

class SimpleValueFuture[A](a: A) extends Future[A] {
  def onComplete[U](f: Either[Throwable, A] => U) { f(Right(a)) } // FIXME: should this swallow exceptions?
  def result() = Some(a)
  def await() {}
}
