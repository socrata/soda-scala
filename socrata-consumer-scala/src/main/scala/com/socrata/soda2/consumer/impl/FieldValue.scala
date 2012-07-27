package com.socrata.soda2.consumer
package impl

import com.socrata.soda2.{ColumnNameLike, ColumnName, Util}

case class FieldValue(column: ColumnName, value: String)

object FieldValue {
  implicit def stringpair2fv[T](colVal: (T, String))(implicit ev: ColumnNameLike[T]) =
    FieldValue(ev.asColumnName(colVal._1), Util.quote(colVal._2))

  implicit def numberpair2fv[T](colVal: (T, Number))(implicit ev: ColumnNameLike[T]) =
    FieldValue(ev.asColumnName(colVal._1), colVal._2.toString)

  implicit def bytepair2fv[T](colVal: (T, Byte))(implicit ev: ColumnNameLike[T]) =
    FieldValue(ev.asColumnName(colVal._1), colVal._2.toString)

  implicit def shortpair2fv[T](colVal: (T, Short))(implicit ev: ColumnNameLike[T]) =
    FieldValue(ev.asColumnName(colVal._1), colVal._2.toString)

  implicit def intpair2fv[T](colVal: (T, Int))(implicit ev: ColumnNameLike[T]) =
    FieldValue(ev.asColumnName(colVal._1), colVal._2.toString)

  implicit def longpair2fv[T](colVal: (T, Long))(implicit ev: ColumnNameLike[T]) =
    FieldValue(ev.asColumnName(colVal._1), colVal._2.toString)

  implicit def floatpair2fv[T](colVal: (T, Float))(implicit ev: ColumnNameLike[T]) =
    FieldValue(ev.asColumnName(colVal._1), colVal._2.toString)

  implicit def doublepair2fv[T](colVal: (T, Double))(implicit ev: ColumnNameLike[T]) =
    FieldValue(ev.asColumnName(colVal._1), colVal._2.toString)
}
