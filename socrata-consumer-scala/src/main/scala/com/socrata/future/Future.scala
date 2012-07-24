package com.socrata
package future

import com.ning.http.client.ListenableFuture
import java.util.concurrent.{CountDownLatch, ExecutionException, Executor}

object Future {
  def apply[A](a: =>A)(implicit execService: ExecutionContext): Future[A] = {
    val promise = new Promise[A]
    execService.executor.execute(new Runnable {
      def run() {
        try {
          promise.fulfill(a)
        } catch {
          case e: Throwable =>
            promise.break(e)
        }
      }
    })
    promise.future
  }
}

trait Future[+A] {
  def onComplete[U](f: Either[Throwable, A] => U)
  def result(): Option[A]
  def await()

  def flatMap[B](f: A => Future[B]): Future[B] = {
    val promise = new Promise[B]
    onComplete {
      case Left(err) =>
        promise.break(err)
      case Right(interim) =>
        var good = true
        val newFuture = try {
          f(interim)
        } catch {
          case e: Throwable =>
            promise.break(e)
            good = false
            null
        }
        if(good) {
          newFuture.onComplete {
            case Left(err) => promise.break(err)
            case Right(value) => promise.fulfill(value)
          }
        }
    }
    promise.future
  }

  def map[B](f: A => B): Future[B] = {
    val promise = new Promise[B]

    def complete(e: Either[Throwable, A]) {
      e match {
        case Left(err) =>
          promise.break(err)
        case Right(interim) =>
          val result = try {
            f(interim)
          } catch {
            case e: Throwable =>
              promise.break(e)
              return
          }
          promise.fulfill(result)
      }
    }

    onComplete(complete)
    promise.future
  }

  def filter(f: A => Boolean): Future[A] = map { a =>
    if(f(a)) a
    else throw new NoSuchElementException("Excluded by filter")
  }

  def withFilter(f: A => Boolean): Future[A] = filter(f)

  def apply(): A = {
    result() match {
      case Some(r) => r
      case None => await(); apply()
    }
  }
}

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
    wereWaiting.foreach(_(result))
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
      if(goNow) res(self.result)
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

trait ExecutionContext {
  def executor: Executor
}

object ExecutionContext {
  implicit val defaultExecutionContext = new ExecutionContext {
    val executor = new Executor {
      def execute(command: Runnable) {
        command.run()
      }
    }
  }
}

object WrappedFuture {
  def apply[A](underlying: ListenableFuture[A])(implicit exeuctionContext: ExecutionContext): Future[A] = {
    val promise = new Promise[A]
    underlying.addListener(new Runnable {
      override def run() {
        try {
          promise.fulfill(underlying.get())
        } catch {
          case e: ExecutionException =>
            promise.break(e.getCause)
        }
      }
    }, exeuctionContext.executor)
    promise.future
  }
}
