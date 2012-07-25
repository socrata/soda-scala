package com.socrata.iteratee

import java.io.EOFException

class IdentityIteratee[T] extends Iteratee[T, T] {
  def process(value: T) = Right(value)
  def endOfInput() = throw new EOFException("No value received before end of input")
}
