package com.socrata.future

import java.util.concurrent.CountDownLatch

class Promise[A] { self =>
  private var listeners: List[Either[Throwable, A] => _] = Nil
  private var result: Either[Throwable, A] = _

  def break(err: Throwable) { setResult(Left(err)) }
  def fulfill(result: A) { setResult(Right(result)) }

  private def setResult(r: Either[Throwable, A]) {
    val wereWaiting = synchronized {
      result = r
      val go = listeners
      listeners = Nil
      go
    }
    wereWaiting.foreach(executeOnResult)
  }

  private def executeOnResult(listener: Either[Throwable, A] => _) {
    try {
      listener(result)
    } catch {
      case t: Throwable =>
        // FIXME something more than this
        t.printStackTrace()
    }
  }

  def future: Future[A] = new Future[A] {
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
  }
}
