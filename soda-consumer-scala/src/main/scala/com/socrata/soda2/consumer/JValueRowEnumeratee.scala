package com.socrata.soda2.consumer

import com.rojoma.json.v3.ast._

import com.socrata.iteratee.Iteratee

class JValueRowEnumeratee[T](rowDecoder: JObject => Row, iteratee: Iteratee[Row, T]) extends Iteratee[JValue, T] {
  def process(value: JValue) = value match {
    case rawRow: JObject =>
      val row = rowDecoder(rawRow)
      iteratee.process(row).left.map(new JValueRowEnumeratee(rowDecoder, _))
    case other =>
      throw new InvalidRowJsonException(other, "Found a non-Object in the list of rows")
  }

  def endOfInput() = iteratee.endOfInput()
}
