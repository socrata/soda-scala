package com.socrata.future

import java.util.concurrent.{TimeUnit, ScheduledExecutorService, Executor}

class JavaExecutorExecutionContext[+T <: Executor](val asJava: T) extends ExecutionContext {
  def execute[A](a: => A): Future[A] = {
    val promise = new Promise[A]
    asJava.execute(promiseExecution(promise, a))
    promise.future
  }

  def in[A](seconds: Int)(a: => A): Future[A] = {
    val promise = new Promise[A]
    asJava match {
      case scheduledExecutor: ScheduledExecutorService =>
        scheduledExecutor.schedule(promiseExecution(promise, a), seconds, TimeUnit.SECONDS)
      case other: Executor =>
        other.execute(promiseExecution(promise, { Thread.sleep(1000 * seconds); a }))
    }
    promise.future
  }

  private def promiseExecution[A](promise: Promise[A], a: => A) =
    new Runnable {
      def run() {
        try {
          promise.fulfill(a)
        } catch {
          case t: Throwable =>
            promise.break(t)
        }
      }
    }
}
