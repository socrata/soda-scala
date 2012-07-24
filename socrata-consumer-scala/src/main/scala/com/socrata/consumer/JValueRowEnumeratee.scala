package com.socrata
package consumer

import com.rojoma.json.ast._

import iteratee.Iteratee

class JValueRowEnumeratee[T](iteratee: Iteratee[Row, T]) extends Iteratee[JValue, T] {
  def process(value: JValue) = value match {
    case v@JObject(fields) =>
      val row = new Row {
        def columnTypes = fields.mapValues(_ => "JValue").toMap

        def apply(columnName: String) = fields.getOrElse(columnName, JNull)

        override def toString = v.toString

        def asJObject = v
      }
      iteratee.process(row).left.map(new JValueRowEnumeratee(_))
    case _ =>
      error("NYI")
  }

  def endOfInput() = iteratee.endOfInput()
}
