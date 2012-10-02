package com.socrata.future

import impl.{SimpleValueFuture, SimpleFailureFuture}

object Future {
  /** Evaluate the given expression in the background.
   *
   * @param a The expression to evaluate
   * @param executionContext A strategy for starting asynchronous tasks.
   * @return A [[com.socrata.future.Future]] which will eventually hold the result.
   */
  def apply[A](a: =>A)(implicit executionContext: ExecutionContext): Future[A] = {
    executionContext.execute(a)
  }

  /** Evaluates the given expression on the current thread and creates a [[com.socrata.future.Future]]
   * which can simply be used to access it.
   *
   * @param a The expression to evaluate
   * @return A [[com.socrata.future.Future]] which holds either the result or the exception produced by the evaluation of `a`.
   */
  def now[A](a: =>A): Future[A] = try {
    new SimpleValueFuture(a)
  } catch {
    case e: Throwable =>
      new SimpleFailureFuture(e)
  }

  def sequence[T](futures: Seq[Future[T]]): Future[Seq[T]] = {
    def loop(done: List[T], remaining: List[Future[T]]): Future[Seq[T]] = remaining match {
      case hd :: tl =>
        hd.flatMap { hdValue =>
          loop(hdValue :: done, tl)
        }
      case Nil =>
        now(done.reverse)
    }
    loop(Nil, futures.toList)
  }
}

trait Future[+A] {
  /** Registers a callback to be invoked when this future is completed.  There are no guarantees of ordering
   * or thread-affinity with respect to different listeners.  If the `Future` is already complete, this may or
   * may not execute the given function on the current thread. */
  def onComplete[U](f: Either[Throwable, A] => U)

  /** Returns the result, if one exists.  If the `Future` has not completed, returns None.  If the `Future` has
   * failed, re-throws the exception which caused the failure. */
  def result(): Option[A]

  /** Wait for the future to complete. */
  def await()

  /** Registers a callback to be invoked only if the `Future` completes successfully. */
  def onSuccess[U](f: A => U) = onComplete {
    case Right(a) => f(a)
    case _ => // nothing
  }

  /** Registers a callback to be invoked only if the `Future` does not complete. */
  def onFailure[U](f: Throwable => U) = onComplete {
    case Left(e) => f(e)
    case _ => // nothing
  }

  /** If this is a `Future[Future[T]]`, returns a `Future[T]`. */
  def flatten[B](implicit ev: A => Future[B]): Future[B] = flatMap { x => x }

  /** Returns a new `Future` which is the result of applying the given function
   * to the successfully completed result of this `Future`. */
  def flatMap[B](f: A => Future[B]): Future[B] = {
    val promise = new Promise[B]

    onComplete {
      case Left(err) =>
        promise.break(err)
      case Right(interim) =>
        var good = true
        val newFuture = try {
          f(interim)
        } catch {
          case e: Throwable =>
            promise.break(e)
            good = false
            null
        }
        if(good) {
          newFuture.onComplete {
            case Left(err) => promise.break(err)
            case Right(value) => promise.fulfill(value)
          }
        }
    }

    promise.future
  }

  /** Returns a new `Future` which contains the result of applying the given function
   * to the successfully completed result of this `Future`. */
  def map[B](f: A => B): Future[B] = {
    val promise = new Promise[B]

    def complete(x: Either[Throwable, A]) {
      x match {
        case Left(err) =>
          promise.break(err)
        case Right(interim) =>
          val result = try {
            f(interim)
          } catch {
            case e: Throwable =>
              promise.break(e)
              return
          }
          promise.fulfill(result)
      }
    }

    onComplete(complete)

    promise.future
  }

  /** Produces a new `Future` which holds the succesfully completed result of this future if the
   * given function evalutates to true, or an unsuccessful `Future` which throws [[java.util.NoSuchElementException]]
   * if it does not. */
  def filter(f: A => Boolean): Future[A] = map { a =>
    if(f(a)) a
    else throw new NoSuchElementException("Excluded by filter")
  }

  /** An alias for `filter`. */
  def withFilter(f: A => Boolean): Future[A] = filter(f)

  /** Blocks until this `Future` has completed and then returns its result. */
  def apply(): A = {
    result() match {
      case Some(r) => r
      case None => await(); apply()
    }
  }

  implicit def executionContext: ExecutionContext
}
