package com.socrata.soda2.consumer

import com.rojoma.json.ast._

import com.socrata.iteratee.Iteratee
import com.socrata.soda2.{ColumnName, ColumnNameLike}

class JValueRowEnumeratee[T](columnMap: Map[ColumnName, String], iteratee: Iteratee[Row, T]) extends Iteratee[JValue, T] {
  def process(value: JValue) = value match {
    case v@JObject(fields) =>
      val row = new Row {
        def columnTypes = columnMap

        def apply[C: ColumnNameLike](columnName: C) = get(columnName) match {
          case Some(v) => v
          case None => throw new NoSuchElementException("No column " + columnName)
        }

        def get[C](columnName: C)(implicit ev: ColumnNameLike[C]) = {
          val colName = ev.asColumnName(columnName)
          fields.get(colName.toString) match {
            case s@Some(_) =>
              s
            case None =>
              if(columnTypes.contains(colName)) Some(JNull)
              else None
          }
        }
        def getOrElse[C: ColumnNameLike](columnName: C, orElse: =>JValue) =
          get(columnName).getOrElse(orElse)

        override def toString = v.toString

        def asJObject = v
      }
      iteratee.process(row).left.map(new JValueRowEnumeratee(columnMap, _))
    case other =>
      throw new InvalidRowJsonException(other, "Found a non-Object in the list of rows")
  }

  def endOfInput() = iteratee.endOfInput()
}
