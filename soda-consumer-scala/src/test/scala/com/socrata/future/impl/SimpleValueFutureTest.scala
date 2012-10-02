package com.socrata.future.impl

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

class SimpleValueFutureTest extends WordSpec with MustMatchers {
  "A SimpleValueFuture" should {
    "return the given value" in {
      new SimpleValueFuture(5).result() must be (Some(5))
    }

    "return immediately from await" in {
      new SimpleValueFuture(5).await()
    }

    "immediately invoke onComplete with a Right value" in {
      var completed = 0
      new SimpleValueFuture(5).onComplete { x =>
        x must be (Right(5))
        completed += 1
      }
      completed must be (1)
    }
  }
}
