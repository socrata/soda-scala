package com.socrata.future

import java.util.concurrent.Executor

trait ExecutionContext {
  def asJava: Executor
  def execute[A](a: =>A): Future[A]
  def in[A](seconds: Int)(a: =>A): Future[A]
}

object ExecutionContext {
  object implicits {
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
