package com.socrata.future

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Promise, ExecutionContext, Future}
import java.util.concurrent.{Executor, TimeUnit, ScheduledExecutorService}
import scala.util.Try

trait ScheduledExecutionContext {
  def schedule[T](in: FiniteDuration)(f: => T): Future[T]
}

class WrappedScheduledExecutionContext(executor: ScheduledExecutorService, reporter: Throwable => Unit = ExecutionContext.defaultReporter) extends ExecutionContext with ScheduledExecutionContext with Executor {
  def execute(runnable: Runnable) {
    executor.execute(runnable)
  }

  def reportFailure(t: Throwable) {
    reporter(t)
  }

  def schedule[T](in: FiniteDuration)(f: => T): Future[T] = {
    val promise = Promise[T]()
    val r = new Runnable {
      def run() {
        try {
          promise.complete(Try(f))
        } catch {
          case t: Throwable =>
            reportFailure(t)
        }
      }
    }
    executor.schedule(r, in.toMillis, TimeUnit.MILLISECONDS)
    promise.future
  }
}
