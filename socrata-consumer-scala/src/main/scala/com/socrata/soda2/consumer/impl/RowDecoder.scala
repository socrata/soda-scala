package com.socrata.soda2.consumer
package impl

import java.net.URI

import com.rojoma.json.ast.{JString, JObject}

import com.socrata.soda2.{ColumnNameLike, ColumnName}
import com.socrata.soda2.values.{SodaValue, SodaType}

class RowDecoder(datasetBase: URI, schema: Map[ColumnName, SodaType]) extends (JObject => Row) {
  def apply(rawRow: JObject): Row = new Row {
    def columnTypes = schema

    def apply[T](columnNameRaw: T, throwOnUnknownColumn: Boolean)(implicit ev: ColumnNameLike[T]) = {
      val colName = ev.asColumnName(columnNameRaw)
      schema.get(colName) match {
        case Some(typ) =>
          rawRow.get(colName.toString) match {
            case Some(value) =>
              typ.convertFrom(value)
            case None =>
              None
          }
        case None =>
          if(throwOnUnknownColumn) throw new IllegalArgumentException("Unknown column " + JString(colName.toString))
          else None
      }
    }

    def getOrElse[T](columnNameRaw: T, orElse: => SodaValue, throwOnUnknownColumn: Boolean)(implicit ev: ColumnNameLike[T]) = {
      val colName = ev.asColumnName(columnNameRaw)
      schema.get(colName) match {
        case Some(typ) =>
          rawRow.get(colName.toString) match {
            case Some(value) =>
              typ.convertFrom(value).getOrElse(orElse)
            case None =>
              orElse
          }
        case None =>
          if(throwOnUnknownColumn) throw new IllegalArgumentException("Unknown column " + JString(colName.toString))
          else orElse
      }
    }

    def asJObject = rawRow

    def asMap = schema.map { case (k, _) =>
      k -> apply(k)
    }
  }
}
