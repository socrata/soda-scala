package com.socrata.soda2.consumer.impl

import com.socrata.soda2.ColumnName

case class FieldValue(column: ColumnName, value: String)

object FieldValue {
  implicit def ss2fv(colVal: (String, String)) = FieldValue(ColumnName(colVal._1), colVal._2)
  implicit def cs2fv(colVal: (ColumnName, String)) = FieldValue(colVal._1, colVal._2)
}
