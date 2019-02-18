package com.socrata.soda2.consumer
package impl

import java.net.URI

import com.rojoma.json.ast.{JString, JObject}

import com.socrata.soda2.{UnknownTypeException, ColumnNameLike, ColumnName}
import com.socrata.soda2.values._

class RowDecoder(datasetBase: URI, schema: Map[ColumnName, SodaType]) extends (JObject => Row) {
  def this(datasetBase: URI, rawSchema: Map[ColumnName, String])(implicit disambiguator: QueryDisambiguator) =
    this(datasetBase, RowDecoder.cookSchema(rawSchema))

  def apply(rawRow: JObject): Row = new Row {
    def columnTypes = schema

    def apply[T](columnNameRaw: T, throwOnUnknownColumn: Boolean)(implicit ev: ColumnNameLike[T]) = {
      val colName = ev.asColumnName(columnNameRaw)
      schema.get(colName) match {
        case Some(typ) =>
          rawRow.get(colName.toString) match {
            case Some(value) =>
              // TODO: use "datasetBase" to resolve links
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
              // TODO: use "datasetBase" to resolve links
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

object RowDecoder {
  def cookSchema(rawSchema: Map[ColumnName, String]) =
    rawSchema.mapValues(conversionForType).toMap

  val conversionForType: String => SodaType = { rawType: String =>
    typeMap.getOrElse(rawType, throw new UnknownTypeException(rawType))
  }

  private val typeMap = Map(
    "row_identifier" -> SodaRowIdentifier,
    "row_version" -> SodaRowVersion,
    "text" -> SodaString,
    "blob" -> SodaBlob,
    "link" -> SodaLink,
    "number" -> SodaNumber,
    "double" -> SodaDouble,
    "money" -> SodaMoney,
    "geospatial" -> SodaGeospatial,
    "location" -> SodaLocation,
    "boolean" -> SodaBoolean,
    "checkbox" -> SodaBoolean, // old name
    "floating_timestamp" -> SodaFloatingTimestamp,
    "calendar_date" -> SodaFloatingTimestamp, // old name
    "fixed_timestamp" -> SodaFixedTimestamp,
    "array" -> SodaArray,
    "object" -> SodaObject,
    "point" -> SodaPoint,
    "multi_point" -> SodaMultiPoint,
    "line_string" -> SodaLineString,
    "multi_line_string" -> SodaMultiLineString,
    "polygon" -> SodaPolygon,
    "multi_polygon" -> SodaMultiPolygon
  )
}
