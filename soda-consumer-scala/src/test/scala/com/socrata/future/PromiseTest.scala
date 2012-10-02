package com.socrata.future

import java.util.concurrent.{TimeUnit, Semaphore}

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

import ExecutionContext.implicits._

class PromiseTest extends WordSpec with MustMatchers {
  case class Failure() extends Exception

  "A promise" should {
    "not have a value before fulfillment or breakage" in {
      val p = new Promise[Int]
      p.future.result must be (None)
    }

    "fire onComplete on fulfillment" in {
      val completedRan = new Semaphore(0)
      var completed = 0
      val p = new Promise[Int]
      p.future.onComplete { x =>
        x must be (Right(5))
        completed += 1
        completedRan.release()
      }
      completed must equal (0)
      p.fulfill(5)
      completedRan.tryAcquire(100, TimeUnit.MILLISECONDS) must be (true)
      completed must equal (1)
    }

    "fire onComplete on breakage" in {
      val completedRan = new Semaphore(0)
      var completed = 0
      val p = new Promise[Int]
      p.future.onComplete { x =>
        x must be (Left(Failure()))
        completed += 1
        completedRan.release()
      }
      completed must equal (0)
      p.break(Failure())
      completedRan.tryAcquire(100, TimeUnit.MILLISECONDS) must be (true)
      completed must equal (1)
    }

    "have a value after fulfillment" in {
      val p = new Promise[Int]
      p.fulfill(5)
      p.future.result() must be (Some(5))
    }

    "have a failure after breakage" in {
      val p = new Promise[Int]
      p.break(new Failure)
      evaluating { p.future.result() } must produce[Failure]
    }
  }
}
