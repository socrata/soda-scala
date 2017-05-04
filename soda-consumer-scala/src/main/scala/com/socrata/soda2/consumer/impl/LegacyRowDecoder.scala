package com.socrata.soda2.consumer
package impl

import java.net.URI

import com.rojoma.json.ast.{JObject, _}
import com.rojoma.json.io.{JsonReader, JsonReaderException}
import com.socrata.soda2.ColumnName
import com.socrata.soda2.values._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}

private[consumer] object LegacyRowDecoder {

  def apply(datasetBase: URI, rawSchema: Map[ColumnName, String]): JObject => Row =
    sodaify(datasetBase, rawSchema).andThen(new RowDecoder(datasetBase, convertTypes(rawSchema)))

  def convertTypes(rawSchema: Map[ColumnName, String]): Map[ColumnName, SodaType] =
    for {
      (col, rawType) <- rawSchema
      typeF <- typeMap.get(rawType)
    } yield col -> typeF(col.toString)

  def sodaify(datasetBase: URI, rawSchema: Map[ColumnName, String]): JObject => JObject = {
    val schema =
      for {
        (k, v) <- rawSchema
        if typeMap.contains(k.toString)
      } yield k.toString -> v

    json => JObject {
      for {
        (field, rawValue) <- json.fields
        tpe <- schema.get(field)
        value = conversionForValue(tpe)(field, datasetBase, rawValue)
        if value != JNull
      } yield field -> value
    }
  }

  def k(x: SodaType) = (_: String) => x

  private val typeMap = Map[String, String => SodaType] (
    "text" -> k(SodaString),
    "html" -> k(SodaString),
    "number" -> k(SodaNumber),
    "double" -> k(SodaDouble),
    "money" -> k(SodaMoney),
    "percent" -> k(SodaNumber),
    "date" -> k(SodaFixedTimestamp),
    "calendar_date" -> k(SodaFloatingTimestamp),
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
  )

  def selectMetadataType(columnName: String): SodaType = columnName match {
    case ":id" => SodaNumber
    case ":created_at" => SodaFixedTimestamp
    case ":position" => SodaNumber
    case ":meta" => SodaString
    case ":created_meta" => SodaString
    case ":updated_at" => SodaFixedTimestamp
    case ":updated_meta" => SodaFixedTimestamp
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
    "url" -> id,
    "email" -> id,
    "checkbox" -> id,
    "flag" -> id,
    "stars" -> stars2num,
    "phone"-> id,
    "drop_down_list" -> id,
    "photo" -> photo2link,
    "document" -> doc2obj,
    "nested_table" -> id,
    "meta_data" -> metadata2thing
  )

  def metadata2thing(fieldName: String, uri: URI, value: JValue): JValue = fieldName match {
    case ":id" => value match {
      case JNumber(x) => JString(x.toString)
      case JNull => JNull
      case _ => sys.error("nyi")
    }
    case ":created_at" => epoch2iso(fieldName, uri, value)
    case ":position" => value match {
      case JNumber(x) => JString(x.toString)
      case JNull => JNull
      case _ => sys.error("nyi")
    }
    case ":meta" => value
    case ":created_meta" => value
    case ":updated_at" => epoch2iso(fieldName, uri, value)
    case ":updated_meta" => value
    case _ => sys.error("nyi")
  }

  def stars2num(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JNumber(n) => JString(n.toString)
    case JString(s) => JString(s) // already converted
    case _ => sys.error("NYI")
  }

  def numberify(value: JValue) = value match {
    case JNumber(n) =>
      JNumber(n)
    case JString(n) =>
      try {
        JNumber(BigDecimal(n))
      } catch {
        case _: NumberFormatException =>
          sys.error("NYI")
      }
    case _ =>
      sys.error("NYI")
  }

  def loc2loc(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JObject(fields) =>
      // A SODA2 location is *very nearly* a SODA1 location.
      // The only differences are that the lat/lon values are doubles (i.e., JSON
      // numbers) instead of numbers (i.e., JSON strings) and that the human_address
      // field is a JSON object, not a string-containing-a-JSON-object.
      val newFields = new scala.collection.mutable.HashMap[String, JValue]
      fields.get("latitude").foreach { l => newFields += "latitude" -> numberify(l) }
      fields.get("longitude").foreach { l => newFields += "longitude" -> numberify(l) }
      try {
        fields.get("human_address").map {
          case JString(s) => JsonReader.fromString(s)
          case other => other
        }.foreach { ha =>
          newFields += "human_address" -> ha
        }
      } catch {
        case e: JsonReaderException =>
          throw new MalformedJsonWhileReadingRowsException(e)
      }
      if(newFields.isEmpty) JNull
      else JObject(newFields)
    case _ =>
      sys.error("NYI")
  }

  def epoch2iso(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JString(s) =>
      try {
        JString(ISODateTimeFormat.dateTime.print(new DateTime(s.toLong * 1000L, DateTimeZone.UTC)))
      } catch {
        case _: NumberFormatException =>
          JString(s) // it's probably already formatted properly so fall through to standard SODA2 parsing
      }
    case n: JNumber =>
      JString(ISODateTimeFormat.dateTime.print(new DateTime(n.toLong * 1000L, DateTimeZone.UTC)))
    case JNull => JNull
    case _ => sys.error("NYI")
  }

  def photo2link(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JString(fileIdOrUri) =>
      if(fileIdOrUri.startsWith("/api/")) JString(uri.resolve(fileIdOrUri).toString)
      else JString(uri.resolve("/api/file_data/" + fileIdOrUri).toString)
    case _ => sys.error("nyi")
  }

  def doc2obj(fieldName: String, uri: URI, value: JValue): JValue = value match {
    case JObject(fields) =>
      val result = new scala.collection.mutable.HashMap[String, JValue]
      if(fields.contains("FileName") || fields.contains("Url") || fields.contains("MimeType") || fields.contains("Size"))
        JObject(fields)
      else {
        fields.get("file_id") match {
          case Some(JString(fileId)) => result += "url" -> JString(uri.resolve("/api/file_data/" + fileId).toString)
          case None => // nothing
          case _ => sys.error("nyi")
        }
        for(ct <- fields.get("content_type")) result += "content_type" -> ct
        for(fn <- fields.get("filename")) result += "filename" -> fn
        for(sz <- fields.get("size")) result += "size" -> sz
        if(result.isEmpty) JNull
        else JObject(result)
      }
    case _ => sys.error("nyi")
  }
}
