package com.socrata.future

import java.util.concurrent.Executor

/** Represents a strategy for running tasks asynchronously. */
trait ExecutionContext {
  /** Represents this `ExecutionContext` as a Java [[java.util.concurrent.Executor]]. */
  def asJava: Executor

  /** Starts a task. */
  def execute[A](a: =>A): Future[A]

  /** Starts a task after the given number of seconds has passed. */
  def in[A](seconds: Int)(a: =>A): Future[A]
}

object ExecutionContext {
  object implicits {
    /** An execution context which runs all tasks on the current thread. */
    implicit val defaultExecutionContext = new ExecutionContext {
      val asJava = new Executor {
        def execute(command: Runnable) {
          command.run()
        }
      }

      def execute[A](a: =>A) = Future.now(a)

      def in[A](seconds: Int)(a: =>A) = Future.now {
        Thread.sleep(seconds * 1000L)
        a
      }
    }
  }
}
