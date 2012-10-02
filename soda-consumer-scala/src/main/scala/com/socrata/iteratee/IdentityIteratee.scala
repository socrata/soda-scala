package com.socrata.iteratee

import java.io.EOFException

/** An [[com.socrata.iteratee.Iteratee]] that simply finishes with the first value passed to it.
 */
class IdentityIteratee[T] extends Iteratee[T, T] {
  /** Immediately signals completion of computation. */
  def process(value: T) = Right(value)

  /** Signals that no input was ever received by throwing an exception.
   * @throws EOFException
   */
  def endOfInput() = throw new EOFException("No value received before end of input")
}
