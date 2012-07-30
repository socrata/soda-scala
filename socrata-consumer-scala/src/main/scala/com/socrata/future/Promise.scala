package com.socrata.future

import java.util.concurrent.CountDownLatch

/** An object that represents the promise to produce a result
 * at some point in the future. */
class Promise[A](implicit executionContext: ExecutionContext) { self =>
  private var listeners: List[Either[Throwable, A] => _] = Nil
  private var result: Either[Throwable, A] = _

  /** Break the promise by setting the result to an exception to be
   * re-thrown when the promise is demanded.
   *
   * @throws IllegalStateException if the promise has already been fulfilled or broken. */
  def break(err: Throwable) { setResult(Left(err)) }

  /**Fulfill the promise by setting the result to the given value.
   *
   * @throws IllegalStateException if the promise has already been fulfilled or broken. */
  def fulfill(result: A) { setResult(Right(result)) }

  private def setResult(r: Either[Throwable, A]) {
    val wereWaiting = synchronized {
      if(result != null) throw new IllegalStateException("Promise has already been fulfilled or broken.")
      result = r
      val go = listeners
      listeners = Nil
      go
    }
    wereWaiting.foreach(executeOnResult)
  }

  private def executeOnResult(listener: Either[Throwable, A] => _) {
    executionContext.execute {
      try {
        listener(result)
      } catch {
        case t: Throwable =>
          // FIXME something more than this
          t.printStackTrace()
      }
    }
  }

  /** A [[com.socrata.future.Future]] which can be used to access the result of this promise. */
  val future: Future[A] = new Future[A] {
    def onComplete[B](res: Either[Throwable, A] => B) {
      val goNow = self.synchronized {
        if(self.result == null) {
          listeners ::= res
          false
        } else {
          true
        }
      }
      if(goNow) executeOnResult(res)
    }

    def result(): Option[A] = {
      val r = synchronized {
        if(self.result == null) return None
        self.result
      }
      r match {
        case Left(t) => throw t
        case Right(x) => Some(x)
      }
    }

    def await() {
      val join = new CountDownLatch(1)
      onComplete { _ => join.countDown() }
      join.await()
    }

    def executionContext = Promise.this.executionContext
  }
}
