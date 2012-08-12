package com.socrata.soda2.consumer
package impl

import java.net.URI

import com.rojoma.json.ast._

import com.socrata.soda2.{UnknownTypeException, ColumnName}
import com.socrata.soda2.values._
import org.joda.time.format.{DateTimeFormatterBuilder, ISODateTimeFormat}
import com.rojoma.json.io.JsonReader

private[consumer] class LegacyRowDecoder(datasetBase: URI, rawSchema: Map[ColumnName, String]) extends (JObject => Row) {
  import LegacyRowDecoder._

  val rowDecoder = new RowDecoder(datasetBase, rawSchema.mapValues(conversionForType).toMap)

  val schema = rawSchema.map { case (k,v) => k.toString -> v }

  def apply(rawRow: JObject): Row = {
    val soda2ified = rawRow.fields.collect { case (field, rawValue) if schema.contains(field) =>
      field -> conversionForValue(schema(field))(datasetBase, rawValue)
    }
    rowDecoder(JObject(soda2ified))
  }
}

private[consumer] object LegacyRowDecoder {
  val conversionForType: String => SodaType = { rawType: String =>
    typeMap.getOrElse(rawType, throw new UnknownTypeException(rawType))
  }

  private val typeMap = Map(
    "text" -> SodaString,
    "html" -> SodaString,
    "number" -> SodaNumber,
    "double" -> SodaDouble,
    "money" -> SodaMoney,
    "percent" -> SodaNumber,
    "date" -> SodaTimestamp,
    "calendar_date" -> SodaTimestamp, // FIXME
    "location" -> SodaLocation,
    "url" -> SodaObject,
    "email" -> SodaString,
    "checkbox" -> SodaBoolean,
    "flag" -> SodaString,
    "stars" -> SodaNumber,
    "phone"-> SodaObject,
    "drop_down_list" -> SodaString,
    "photo" -> SodaLink,
    "document" -> SodaObject,
    "nested_table" -> SodaObject
    // TODO: dataset link, which is a wildly ill-thought-out misfeature
  )

  val id = (_: URI, v: JValue) => v

  // map from SodaTypes to functions which convert (uri, JValue) pairs to JValues
  val conversionForValue: String => (URI, JValue) => JValue = Map[String, (URI, JValue) => JValue] (
    "text" -> id,
    "html" -> id,
    "number" -> id,
    "double" -> id,
    "money" -> id,
    "percent" -> id,
    "date" -> epoch2iso _, // FIXME: this needs to return a "fixed" timestamp
    "calendar_date" -> id,
    "location" -> loc2loc,
    "url" -> id,
    "email" -> id,
    "checkbox" -> id,
    "flag" -> id,
    "stars" -> stars2num,
    "phone"-> id,
    "drop_down_list" -> id,
    "photo" -> photo2link _,
    "document" -> doc2obj,
    "nested_table" -> id
  // TODO: dataset link, which is a wildly ill-thought-out misfeature
  )

  def stars2num(uri: URI, value: JValue): JValue = value match {
    case JNumber(n) => JString(n.toString)
    case _ => error("NYI")
  }

  def loc2loc(uri: URI, value: JValue): JValue = value match {
    case JObject(fields) =>
      JObject(fields.get("human_address") match {
        case Some(JString(ha)) => fields + ("human_address" -> JsonReader.fromString(ha))
        case None => fields
        case _ => error("NYI")
      })
    case _ =>
      error("NYI")
  }

  val epochFormatter = new DateTimeFormatterBuilder().append(ISODateTimeFormat.date()).appendLiteral('T').append(ISODateTimeFormat.hourMinuteSecond()).toFormatter
  def formatEpoch(instant: Long) = {
    JString(epochFormatter.print(instant))
  }

  def epoch2iso(uri: URI, value: JValue): JValue = value match {
    case JString(num) =>
      try {
        formatEpoch(num.toLong * 1000L)
      } catch {
        case _: NumberFormatException => error("NYI")
      }
    case n: JNumber =>
      formatEpoch(n.toLong * 1000L)
    case JNull => JNull
    case _ => error("NYI")
  }

  def photo2link(uri: URI, value: JValue): JValue = value match {
    case JString(fileId) => JString(uri.resolve("/api/file_data/" + fileId).toString)
    case _ => error("nyi")
  }

  def doc2obj(uri: URI, value: JValue): JValue = value match {
    case JObject(fields) =>
      val result = new scala.collection.mutable.HashMap[String, JValue]
      fields.get("file_id") match {
        case Some(JString(fileId)) => result += "url" -> JString(uri.resolve("/api/file_data/" + fileId).toString)
        case _ => error("nyi")
      }
      fields.get("filename") match {
        case Some(JString(filename)) => result += "filename" -> JString(filename)
        case _ => error("nyi")
      }
      JObject(result)
    case _ => error("nyi")
  }
}
