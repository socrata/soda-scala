package com.socrata.soda2.consumer
package impl

import java.net.URI

import com.rojoma.json.ast._

import com.socrata.soda2.{UnknownTypeException, ColumnName}
import com.socrata.soda2.values._
import org.joda.time.format.{DateTimeFormatterBuilder, ISODateTimeFormat}
import com.rojoma.json.io.{JsonReaderException, JsonReader}

private[consumer] class LegacyRowDecoder(datasetBase: URI, rawSchema: Map[ColumnName, String]) extends (JObject => Row) {
  import LegacyRowDecoder._

  val rowDecoder = new RowDecoder(datasetBase, rawSchema.map(conversionForType).toMap)

  val schema = rawSchema.map { case (k,v) => k.toString -> v }

  def apply(rawRow: JObject): Row = {
    val soda2ified = rawRow.fields.collect { case (field, rawValue) if schema.contains(field) =>
      field -> conversionForValue(schema(field))(field, datasetBase, rawValue)
    }
    rowDecoder(JObject(soda2ified.filterNot(_._2  == JNull)))
  }
}

private[consumer] object LegacyRowDecoder {
  val conversionForType: ((ColumnName, String)) => (ColumnName, SodaType) = { case (col, rawType) =>
    col -> typeMap.getOrElse(rawType, throw new UnknownTypeException(rawType))(col.toString)
  }

  def k(x: SodaType) = (_: String) => x

  private val typeMap = Map[String, String => SodaType] (
    "text" -> k(SodaString),
    "html" -> k(SodaString),
    "number" -> k(SodaNumber),
    "double" -> k(SodaDouble),
    "money" -> k(SodaMoney),
    "percent" -> k(SodaNumber),
    "date" -> k(SodaTimestampFixed),
    "calendar_date" -> k(SodaTimestampFloating),
    "location" -> k(SodaLocation),
    "url" -> k(SodaObject),
    "email" -> k(SodaString),
    "checkbox" -> k(SodaBoolean),
    "flag" -> k(SodaString),
    "stars" -> k(SodaNumber),
    "phone"-> k(SodaObject),
    "drop_down_list" -> k(SodaString),
    "photo" -> k(SodaLink),
    "document" -> k(SodaObject),
    "nested_table" -> k(SodaObject),
    "meta_data" -> selectMetadataType _
    // TODO: dataset link, which is a wildly ill-thought-out misfeature
  )

  def selectMetadataType(columnName: String): SodaType = columnName match {
    case ":id" => SodaNumber
    case ":created_at" => SodaTimestampFixed
    case ":position" => SodaNumber
    case ":meta" => SodaString
    case ":created_meta" => SodaString
    case ":updated_at" => SodaTimestampFixed
    case ":updated_meta" => SodaTimestampFixed
  }

  val id = (_: String, _: URI, v: JValue) => v

  // map from SodaTypes to functions which convert (uri, JValue) pairs to JValues
  val conversionForValue: String => (String, URI, JValue) => JValue = Map[String, (String, URI, JValue) => JValue] (
    "text" -> id,
    "html" -> id,
    "number" -> id,
    "double" -> id,
    "money" -> id,
    "percent" -> id,
    "date" -> epoch2iso,
    "calendar_date" -> id,
    "location" -> loc2loc,
    "url" -> url2obj,
    "email" -> id,
    "checkbox" -> id,
    "flag" -> id,
    "stars" -> stars2num,
    "phone"-> phone2obj,
    "drop_down_list" -> id,
    "photo" -> photo2link,
    "document" -> doc2obj,
    "nested_table" -> id,
    "meta_data" -> metadata2thing
  // TODO: dataset link, which is a wildly ill-thought-out misfeature
  )

  def metadata2thing(fieldName: String, uri: URI, value: JValue): JValue = fieldName match {
    case ":id" => value match {
      case JNumber(x) => JString(x.toString)
      case JNull => JNull
    }
    case ":created_at" => epoch2iso(fieldName, uri, value)
    case ":position" => value match {
      case JNumber(x) => JString(x.toString)
      case JNull => JNull
    }
    case ":meta" => value
    case ":created_meta" => value
    case ":updated_at" => epoch2iso(fieldName, uri, value)
    case ":updated_meta" => value
  }

  def stars2num(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JNumber(n) => JString(n.toString)
    case JString(s) => JString(s) // already converted
    case _ => error("NYI")
  }

  def loc2loc(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JObject(fields) =>
      val lat = fields.getOrElse("latitude", JNull)
      val lon = fields.getOrElse("longitude", JNull)
      val ha = try {
        fields.get("human_address").collect { case JString(s) => JsonReader.fromString(s) }.getOrElse(JNull)
      } catch {
        case e: JsonReaderException =>
          throw new MalformedJsonWhileReadingRowsException(e)
      }
      if(lat == JNull && lon == JNull && ha == JNull) JNull
      else JArray(Seq(lat, lon, ha))
    case JArray(elems) =>
      JArray(elems)
    case _ =>
      error("NYI")
  }

  def url2obj(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JObject(fields) =>
      val desc = fields.getOrElse("description", JNull)
      val url = fields.getOrElse("url", JNull)
      if(desc == JNull && url == JNull) JNull
      else JObject(Map("Url" -> url, "Description" -> desc))
    case _ =>
      error("NYI")
  }

  def phone2obj(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JObject(fields) =>
      val num = fields.getOrElse("phone_number", JNull)
      val typ = fields.getOrElse("phone_type", JNull)
      if(num == JNull && typ == JNull) JNull
      else JObject(Map("PhoneNumber" -> num, "Type" -> typ))
    case _ =>
      error("NYI")
  }

  def epoch2iso(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JString(s) =>
      try {
        JString(ISODateTimeFormat.dateTime.print(s.toLong * 1000L))
      } catch {
        case _: NumberFormatException =>
          JString(s) // it's probably already formatted properly so fall through to standard SODA2 parsing
      }
    case n: JNumber =>
      JString(ISODateTimeFormat.dateTime.print(n.toLong * 1000L))
    case JNull => JNull
    case _ => error("NYI")
  }

  def photo2link(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JString(fileIdOrUri) =>
      if(fileIdOrUri.startsWith("/api/")) JString(uri.resolve(fileIdOrUri).toString)
      else JString(uri.resolve("/api/file_data/" + fileIdOrUri).toString)
    case _ => error("nyi")
  }

  def doc2obj(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JObject(fields) =>
      val result = new scala.collection.mutable.HashMap[String, JValue]
      if(fields.contains("FileName") || fields.contains("Url") || fields.contains("MimeType") || fields.contains("Size"))
        JObject(fields)
      else {
        fields.get("file_id") match {
          case Some(JString(fileId)) => result += "Url" -> JString(uri.resolve("/api/file_data/" + fileId).toString)
          case None => // nothing
          case _ => error("nyi")
        }
        fields.get("content_type") match {
          case Some(JString(contentType)) => result += "MimeType" -> JString(contentType)
          case None => // nothing
          case _ => error("nyi")
        }
        fields.get("filename") match {
          case Some(JString(filename)) => result += "FileName" -> JString(filename)
          case None => // nothing
          case _ => error("nyi")
        }
        fields.get("size") match {
          case Some(JNumber(size)) => result += "Size" -> JNumber(size)
          case None => // nothing
          case _ => error("nyi")
        }
        if(result.isEmpty) JNull
        else JObject(result)
      }
    case _ => error("nyi")
  }
}
