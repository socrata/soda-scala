package com.socrata.soda2.consumer

import com.rojoma.json.ast._

import com.socrata.iteratee.Iteratee
import com.socrata.soda2.{ColumnName, ColumnNameLike}

class JValueRowEnumeratee[T](iteratee: Iteratee[Row, T]) extends Iteratee[JValue, T] {
  def process(value: JValue) = value match {
    case v@JObject(fields) =>
      val row = new Row {
        def columnTypes = fields.map { // TODO: compute this once for the entire result set
          case (k, _) => ColumnName(k) -> "JValue"
        } (scala.collection.breakOut)

        // TODO: Remember that not all rows contain values for all columns
        def apply[C](columnName: C)(implicit ev: ColumnNameLike[C]) = fields(ev.asColumnName(columnName).toString)
        def get[C](columnName: C)(implicit ev: ColumnNameLike[C]) = fields.get(ev.asColumnName(columnName).toString)
        def getOrElse[C](columnName: C, orElse: =>JValue)(implicit ev: ColumnNameLike[C]) = fields.getOrElse(ev.asColumnName(columnName).toString, orElse)

        override def toString = v.toString

        def asJObject = v
      }
      iteratee.process(row).left.map(new JValueRowEnumeratee(_))
    case other =>
      throw new InvalidRowJsonException(other, "Found a non-Object in the list of rows")
  }

  def endOfInput() = iteratee.endOfInput()
}
