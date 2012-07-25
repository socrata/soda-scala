package com.socrata.future

import java.util.concurrent.Executor

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
