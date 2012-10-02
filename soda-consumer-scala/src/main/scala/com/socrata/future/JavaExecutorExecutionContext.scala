package com.socrata.future

import java.util.concurrent.{TimeUnit, ScheduledExecutorService, Executor}

/** A [[com.socrata.future.ExecutionContext]] which wraps a Java [[java.util.concurrent.Executor]]. */
class JavaExecutorExecutionContext[+T <: Executor](val asJava: T) extends ExecutionContext {
  def execute[A](a: => A): Future[A] = {
    val promise = new Promise[A]()(this)
    asJava.execute(promiseExecution(promise, a))
    promise.future
  }

  /** Starts a task after the given number of seconds has passed.  If the `Executor` is a
   * [[java.util.concurrent.ScheduledExecutorService]], then it uses the `schedule` method
   * to tell it to run later.  Otherwise it starts a task immediately and sleeps within it. */
  def in[A](seconds: Int)(a: => A): Future[A] = {
    val promise = new Promise[A]()(this)
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
