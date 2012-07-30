package com.socrata.future.impl

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

class SimpleFailureFutureTest extends WordSpec with MustMatchers {
  case class Failure() extends Exception

  "A SimpleFailureFuture" should {
    "rethrow the given exception" in {
      evaluating { new SimpleFailureFuture(Failure()).result() } must produce[Failure]
    }

    "return immediately from await" in {
      new SimpleFailureFuture(Failure()).await()
    }

    "immediately invoke onComplete with a Left value" in {
      var completed = 0
      new SimpleFailureFuture(Failure()).onComplete { x =>
        x must be (Left(Failure()))
        completed += 1
      }
      completed must be (1)
    }
  }
}
