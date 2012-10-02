package com.socrata.iteratee

import com.rojoma.json.util.WrappedCharArray

class StringIteratee(val fragments: Vector[String]) extends CharIteratee[String] {
  def this() = this(Vector.empty)

  def process(input: WrappedCharArray) = Left(new StringIteratee(fragments :+ input.toString))

  def endOfInput() = fragments.mkString
}
