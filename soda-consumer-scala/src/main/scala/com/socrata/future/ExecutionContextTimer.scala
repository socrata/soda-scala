package com.socrata.future

import java.util.{TimerTask, Timer}
import scala.concurrent.{Promise, Future, ExecutionContext}
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class ExecutionContextTimer(val timer: Timer) {
  def in[T](duration: FiniteDuration)(f: => T)(implicit ctx: ExecutionContext): Future[T] = {
    val promise = Promise[T]()
    val task = new TimerTask {
      def run() {
        ctx.execute(new Runnable {
          def run() {
            promise.complete(Try(f))
          }
        })
      }
    }
    timer.schedule(task, duration.toMillis)
    promise.future
  }
}

object ExecutionContextTimer {
  def globalTimer = Implicits.globalTimer
  object Implicits {
    implicit lazy val globalTimer = new ExecutionContextTimer(new Timer(true))
  }
}
