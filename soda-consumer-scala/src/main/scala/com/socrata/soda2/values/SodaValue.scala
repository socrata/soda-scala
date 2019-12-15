package com.socrata.soda2.values

import java.net.URI

import com.rojoma.json.v3.ast._
import com.rojoma.json.v3.matcher._
import org.joda.time.{DateTime, LocalDateTime}
import com.rojoma.json.v3.codec.{DecodeError, JsonDecode, JsonEncode, Path}
import com.rojoma.json.v3.matcher.PObject
import com.rojoma.json.v3.ast.JArray
import com.rojoma.json.v3.ast.JString
import com.rojoma.json.v3.matcher.POption
import com.rojoma.json.v3.util.{AutomaticJsonCodecBuilder, JsonKey}
import org.joda.time.format.ISODateTimeFormat

sealed abstract class SodaValue {
  type ValueType
  def sodaType: SodaType
  def asJson: JValue
  def value:ValueType
}

sealed abstract class SodaType {
  def convertFrom(value: JValue): Option[SodaValue]
}

case class SodaRowIdentifier(value: JValue) extends SodaValue {
  type ValueType = JValue
  def sodaType = SodaRowIdentifier
  def asJson = value
}

case object SodaRowIdentifier extends SodaType with (JValue => SodaRowIdentifier) {
  def convertFrom(value: JValue) = Some(SodaRowIdentifier(value))
  override def toString = "SodaRowIdentifier"
}

case class SodaRowVersion(value: JValue) extends SodaValue {
  type ValueType = JValue
  def sodaType = SodaRowVersion
  def asJson = value
}

case object SodaRowVersion extends SodaType with (JValue => SodaRowVersion) {
  def convertFrom(value: JValue) = Some(SodaRowVersion(value))
  override def toString = "SodaRowVersion"
}

case class SodaString(value: String) extends SodaValue {
  type ValueType = String
  def sodaType = SodaString
  def asJson = JString(value)
}

case object SodaString extends SodaType with (String => SodaString) {

  def convertFrom(value: JValue) = {
    JsonDecode[String].decode(value).right.toOption.map(SodaString)
  }
  override def toString = "SodaString"
}

case class SodaBlob(uri: URI) extends SodaValue {
  type ValueType = URI
  def sodaType = SodaBlob
  def asJson = JString(uri.toString)
  val value = uri
}

case object SodaBlob extends SodaType with (URI => SodaBlob) {
  def convertFrom(value: JValue) = JsonDecode[String].decode(value).right.toOption.map { linkStr =>
    val uri = URI.create(linkStr) // TODO: Catch exceptions
    SodaBlob(uri)
  }
  override def toString = "SodaBlob"
}

case class SodaLink(uri: URI) extends SodaValue {
  type ValueType = URI
  def sodaType = SodaLink
  def asJson = JString(uri.toString)
  val value = uri
}

case object SodaLink extends SodaType with (URI => SodaLink) {
  def convertFrom(value: JValue) = JsonDecode[String].decode(value).right.toOption.map { linkStr =>
    val uri = URI.create(linkStr) // TODO: Catch exceptions
    SodaLink(uri)
  }
  override def toString = "SodaLink"
}

case class SodaNumber(value: BigDecimal) extends SodaValue {
  type ValueType = BigDecimal
  def sodaType = SodaNumber
  def asJson = JString(value.toString) // FIXME: is this right?  It's certainly how we receive it!
}

case object SodaNumber extends SodaType with (BigDecimal => SodaNumber) {
  def convertFrom(value: JValue) = JsonDecode[String].decode(value).right.toOption.map { numStr =>
    new BigDecimal(new java.math.BigDecimal(numStr)) // TODO: Catch exceptions
  }.orElse {
    JsonDecode[BigDecimal].decode(value).right.toOption
  }.map(SodaNumber)

  override def toString = "SodaNumber"
}

case class SodaDouble(value: Double) extends SodaValue {
  type ValueType = Double
  def sodaType = SodaDouble
  def asJson = JNumber(value)
}

case object SodaDouble extends SodaType with (Double => SodaDouble) {
  def convertFrom(value: JValue) = JsonDecode[Double].decode(value).right.toOption.map(SodaDouble)
  override def toString = "SodaDouble"
}

case class SodaMoney(value: BigDecimal) extends SodaValue {
  type ValueType = BigDecimal
  def sodaType = SodaMoney
  def asJson = JString(value.toString) // FIXME: is this right?  It's certainly how we receive it!
}

case object SodaMoney extends SodaType with (BigDecimal => SodaMoney) {
  def convertFrom(value: JValue) = JsonDecode[String].decode(value).right.toOption.map { numStr =>
    val num = new java.math.BigDecimal(numStr) // TODO: Catch exceptions
    SodaMoney(new BigDecimal(num))
  }
  override def toString = "SodaMoney"
}

case class SodaGeospatial(value: JValue) extends SodaValue {
  type ValueType = JValue
  def sodaType = SodaGeospatial
  def asJson = value
}

case object SodaGeospatial extends SodaType with (JValue => SodaGeospatial) {
  def convertFrom(value: JValue) = if(value == JNull) None else Some(SodaGeospatial(value))
  override def toString = "SodaGeospatial"
}

case class SodaLocation(address: Option[String], city: Option[String], state: Option[String], zip: Option[String], coordinates: Option[(Double, Double)]) extends SodaValue {
  type ValueType = SodaLocation
  def sodaType = SodaLocation
  def asJson = SodaLocation.jsonCodec.encode(this)
  def value = this
}

case object SodaLocation extends SodaType with ((Option[String], Option[String], Option[String], Option[String], Option[(Double, Double)]) => SodaLocation) {
  private val address = Variable[String]
  private val city = Variable[String]
  private val state = Variable[String]
  private val zip = Variable[String]
  private val latitude = Variable[Double]
  private val longitude = Variable[Double]
  private val Pattern = PObject(
    "latitude" -> POption(latitude).orNull,
    "longitude" -> POption(longitude).orNull,
    "human_address" -> POption(PObject(
      "address" -> POption(address).orNull,
      "city" -> POption(city).orNull,
      "state" -> POption(state).orNull,
      "zip" -> POption(zip).orNull)).orNull)

  implicit val jsonCodec = new JsonEncode[SodaLocation] with JsonDecode[SodaLocation] {
    def encode(x: SodaLocation) =
      Pattern.generate(address :=? x.address, city :=? x.city, state :=? x.state, zip :=? x.zip,
        latitude :=? x.coordinates.map(_._1),
        longitude :=? x.coordinates.map(_._2))

    def decode(v: JValue) = Pattern.matches(v).right.map {
      case results =>
        val coords = for {
          lat <- latitude.get(results)
          lon <- longitude.get(results)
        } yield (lat, lon)
        SodaLocation(address.get(results), city.get(results), state.get(results), zip.get(results), coords)
    }
  }

  def convertFrom(value: JValue)= {
    JsonDecode[SodaLocation].decode(value).right.toOption
  }

  override def toString = "SodaLocation"
}

case class SodaUrl(url: String, description: Option[String]) extends SodaValue {
  type ValueType = SodaUrl
  def sodaType = SodaUrl
  def asJson = SodaUrl.jsonCodec.encode(this)
  def value = this
}

case object SodaUrl extends SodaType with ((String, Option[String]) => SodaUrl) {
  private val url = Variable[String]
  private val description = Variable[String]
  private val Pattern = PObject(
    "url" -> url,
    "description" -> POption(description).orNull)

  implicit val jsonCodec = new JsonEncode[SodaUrl] with JsonDecode[SodaUrl] {
    def encode(x: SodaUrl) =
      Pattern.generate(url := x.url, description :=? x.description)

    def decode(v: JValue) = Pattern.matches(v).right.map { results =>
      SodaUrl(url(results), description.get(results))
    }
  }

  def convertFrom(value: JValue) = JsonDecode[SodaUrl].decode(value).right.toOption

  override def toString = "SodaUrl"
}

case class SodaBoolean(value: Boolean) extends SodaValue {
  type ValueType = Boolean
  def sodaType = SodaBoolean
  def asJson = JBoolean(value)
}

case object SodaBoolean extends SodaType with (Boolean => SodaBoolean) {
  def convertFrom(value: JValue) = JsonDecode[Boolean].decode(value).right.map(SodaBoolean).right.toOption
  override def toString = "SodaBoolean"
}

private[values] object TimestampCommon {
  val formatter = ISODateTimeFormat.dateTime.withZoneUTC
  def parser = ISODateTimeFormat.dateTimeParser
}

case class SodaFixedTimestamp(value: DateTime) extends SodaValue {
  type ValueType = DateTime
  def sodaType = SodaFixedTimestamp
  def asJson = JString(SodaFixedTimestamp.formatSodaFixedTimestamp(value))
}

case object SodaFixedTimestamp extends SodaType with (DateTime => SodaFixedTimestamp) {
  import TimestampCommon._

  def convertFrom(value: JValue) = for {
    str <- JsonDecode[String].decode(value).right.toOption
    datetime <- parseSodaFixedTimestamp(str)
  } yield new SodaFixedTimestamp(datetime)

  private def formatSodaFixedTimestamp(datetime: DateTime) = formatter.print(datetime)

  private def parseSodaFixedTimestamp(s: String) =
    try {
      Some(parser.parseDateTime(s))
    } catch {
      case _: IllegalArgumentException =>
        None
    }

  override def toString = "SodaFixedTimestamp"
}

case class SodaFloatingTimestamp(value: LocalDateTime) extends SodaValue {
  type ValueType = LocalDateTime
  def sodaType = SodaFloatingTimestamp
  def asJson = JString(SodaFloatingTimestamp.formatSodaFloatingTimestamp(value))
}

case object SodaFloatingTimestamp extends SodaType with (LocalDateTime => SodaFloatingTimestamp) {
  import TimestampCommon._

  def convertFrom(value: JValue) = for {
    str <- JsonDecode[String].decode(value).right.toOption
    localdatetime <- parseSodaFloatingTimestamp(str)
  } yield new SodaFloatingTimestamp(localdatetime)

  private def parseSodaFloatingTimestamp(s: String) =
    try {
      Some(parser.parseLocalDateTime(s))
    } catch {
      case e: IllegalArgumentException => None
    }

  private def formatSodaFloatingTimestamp(ts: LocalDateTime) = formatter.print(ts)

  override def toString = "SodaFloatingTimestamp"
}

case class SodaArray(value: JArray) extends SodaValue {
  type ValueType = JArray
  def sodaType = SodaArray
  def asJson = value
}

case object SodaArray extends SodaType with (JArray => SodaArray) {
  def convertFrom(value: JValue) = value.cast[JArray].map(SodaArray)
  override def toString = "SodaArray"
}

case class SodaObject(value: JObject) extends SodaValue {
  type ValueType = JObject
  def sodaType = SodaObject
  def asJson = value
}

case object SodaObject extends SodaType with (JObject => SodaObject) {
  def convertFrom(value: JValue) = value.cast[JObject].map(SodaObject)
  override def toString = "SodaObject"
}

case class SodaPoint(latitude: Double, longitude: Double) extends SodaValue {
  type ValueType = SodaPoint
  def asJson = SodaPoint.jsonCodec.encode(this)
  def sodaType = SodaPoint
  def value = this
}

object SodaPoint extends SodaType with ((Double, Double) => SodaPoint) {
  private[values] val simplePoint = new JsonEncode[SodaPoint] with JsonDecode[SodaPoint] {
    private val latitude = Variable[Double]
    private val longitude = Variable[Double]
    private val Pattern = PArray(longitude, latitude)

    def encode(x: SodaPoint) =
      Pattern.generate(latitude := x.latitude,
                       longitude := x.longitude)


    def decode(v: JValue) = Pattern.matches(v).right map { results =>
      SodaPoint(latitude(results), longitude(results))
    }
  }

  implicit val jsonCodec = new JsonEncode[SodaPoint] with JsonDecode[SodaPoint] {

    private val point = Variable[SodaPoint](simplePoint)

    private val Pattern = PObject(
      "type" -> "Point",
      "coordinates" -> point
    )

    def encode(x: SodaPoint) = Pattern.generate(point := x)

    def decode(v: JValue) = Pattern.matches(v).right map { results =>
      point(results)
    }
  }

  def convertFrom(value: JValue) = JsonDecode[SodaPoint].decode(value).right.toOption
}

case class SodaMultiPoint(points: Seq[SodaPoint]) extends SodaValue {
  type ValueType = SodaMultiPoint
  def asJson = SodaMultiPoint.jsonCodec.encode(this)
  def sodaType = SodaMultiPoint
  def value = this
}

case object SodaMultiPoint extends SodaType with (Seq[SodaPoint] => SodaMultiPoint) {
  implicit val jsonCodec = new JsonEncode[SodaMultiPoint] with JsonDecode[SodaMultiPoint]
  {
    val points = Variable[Seq[SodaPoint]]
    private val Pattern = PObject(
      "type" -> "MultiPoint",
      "coordinates" -> points
    )

    def encode(x: SodaMultiPoint) = Pattern.generate(points := x.points)

    def decode(v: JValue) = Pattern.matches(v).right map { results =>
      SodaMultiPoint(points(results))
    }
  }

  def convertFrom(value: JValue) = JsonDecode[SodaMultiPoint].decode(value).right.toOption
}

case class SodaLineString(points: Seq[SodaPoint]) extends SodaValue {
  type ValueType = SodaLineString
  def asJson = SodaLineString.jsonCodec.encode(this)
  def sodaType = SodaLineString
  def value = this
}

case object SodaLineString extends SodaType with (Seq[SodaPoint] => SodaLineString) {
  private[values] val simpleLineString = new JsonEncode[SodaLineString] with JsonDecode[SodaLineString] {

    def encode(x: SodaLineString) = {
      JsonEncode.seqEncode[SodaPoint, Seq].encode(x.points)
    }

    def decode(v: JValue) = {
      JsonDecode.seqDecode[SodaPoint, Seq].decode(v).right.map(points => SodaLineString(points))
    }
  }

  implicit val jsonCodec = new JsonEncode[SodaLineString] with JsonDecode[SodaLineString] {
    private val points = Variable[SodaLineString](simpleLineString)
    private val Pattern = PObject(
      "type" -> "LineString",
      "coordinates" -> points
    )

    def encode(x: SodaLineString) = Pattern.generate(points := x)

    def decode(v: JValue) = Pattern.matches(v).right map { results =>
      points(results)
    }
  }

  def convertFrom(value: JValue) = JsonDecode[SodaLineString].decode(value).right.toOption
}

case class SodaMultiLineString(lines: Seq[SodaLineString]) extends SodaValue {
  type ValueType = SodaMultiLineString
  def asJson = SodaMultiLineString.jsonCodec.encode(this)
  def sodaType = SodaMultiLineString
  def value = this
}

case object SodaMultiLineString extends SodaType with (Seq[SodaLineString] => SodaMultiLineString) {

  private[values] val multiLineString = new JsonEncode[SodaMultiLineString] with JsonDecode[SodaMultiLineString] {

    def encode(x: SodaMultiLineString) = {
      JsonEncode.seqEncode[SodaLineString, Seq].encode(x.lines)
    }

    def decode(v: JValue) = {
      JsonDecode.seqDecode[SodaLineString, Seq].decode(v).right.map(lines => SodaMultiLineString(lines))
    }
  }

  implicit val jsonCodec = new JsonEncode[SodaMultiLineString] with JsonDecode[SodaMultiLineString] {
    private val lines = Variable[SodaMultiLineString](multiLineString)

    private val Pattern = PObject(
      "type" -> "MultiLineString",
      "coordinates" -> lines
    )

    def encode(x: SodaMultiLineString): JValue = {
      Pattern.generate(lines := x)
    }

    def decode(v: JValue): Either[DecodeError, SodaMultiLineString] = {
      Pattern.matches(v).right map { results => lines(results) }
    }
  }

  def convertFrom(value: JValue) = {

    JsonDecode[SodaMultiLineString].decode(value).right.toOption
  }
}

case class SodaPolygon(ring: Seq[SodaPoint], holes: Seq[Seq[SodaPoint]]) extends SodaValue {
  type ValueType = SodaPolygon
  def asJson = SodaPolygon.jsonCodec.encode(this)
  def sodaType = SodaPolygon
  def value = this
}

case object SodaPolygon extends SodaType with ((Seq[SodaPoint], Seq[Seq[SodaPoint]]) => SodaPolygon) {
  private[values] val simplePolygon = new JsonEncode[SodaPolygon] with JsonDecode[SodaPolygon] {

    private val ringEncode = JsonEncode.seqEncode[SodaPoint, Seq]
    private val ringDecode = JsonDecode.seqDecode[SodaPoint, Seq]

    private val holesEncode = JsonEncode.seqEncode[Seq[SodaPoint], Seq]
    private val holesDecode = JsonDecode.seqDecode[Seq[SodaPoint], Seq]

    val x1 =  holesDecode.decode(JArray(Seq(JString("a"))))

    def encode(x: SodaPolygon) = {
      val r = ringEncode.encode(x.ring)
      val JArray(hs) = holesEncode.encode(x.holes)
      JArray(r +: hs)
    }

    def decode(v: JValue): Either[DecodeError, SodaPolygon] = v match {
      case JArray(items) if items.nonEmpty =>
        val jr = items.head
        val jhs = items.tail
        for {
          r <- ringDecode.decode(jr).right
          hs <- holesDecode.decode(JArray(jhs)).right
        } yield SodaPolygon(r, hs)
      case _ =>
        Left(DecodeError.InvalidValue(v, Path.empty))
    }
  }

  implicit val jsonCodec = new JsonEncode[SodaPolygon] with JsonDecode[SodaPolygon] {

    val points = Variable[SodaPolygon](simplePolygon)
    private val Pattern = PObject(
      "type" -> "Polygon",
      "coordinates" -> points
    )

    def encode(x: SodaPolygon): JValue = {
      Pattern.generate(points := x)

    }

    def decode(v: JValue):  Either[DecodeError, SodaPolygon] = {
      Pattern.matches(v).right map { results =>
        points(results)
      }
    }
  }

  def convertFrom(value: JValue) = JsonDecode[SodaPolygon].decode(value).right.toOption
}

case class SodaMultiPolygon(polygons: Seq[SodaPolygon]) extends SodaValue {
  type ValueType = SodaMultiPolygon
  def asJson = SodaMultiPolygon.jsonCodec.encode(this)
  def sodaType = SodaMultiPolygon
  def value: SodaMultiPolygon = this
}

case object SodaMultiPolygon extends SodaType with (Seq[SodaPolygon] => SodaMultiPolygon) {

  private[values] val multiPolygon = new JsonEncode[SodaMultiPolygon] with JsonDecode[SodaMultiPolygon] {

    def encode(x: SodaMultiPolygon) = {
      JsonEncode.seqEncode[SodaPolygon, Seq].encode(x.polygons)
    }

    def decode(v: JValue) = {
      JsonDecode.seqDecode[SodaPolygon, Seq].decode(v).right.map(polygons => SodaMultiPolygon(polygons))
    }
  }

  implicit val jsonCodec = new JsonEncode[SodaMultiPolygon] with JsonDecode[SodaMultiPolygon] {
    private val polygons = Variable[SodaMultiPolygon](multiPolygon)

    private val Pattern = PObject(
      "type" -> "MultiPolygon",
      "coordinates" -> polygons
    )

    def encode(x: SodaMultiPolygon): JValue = {
      Pattern.generate(polygons := x)
    }

    def decode(v: JValue): Either[DecodeError, SodaMultiPolygon] = {
      Pattern.matches(v).right map { results => polygons(results) }
    }
  }

  def convertFrom(value: JValue) = {
    JsonDecode[SodaMultiPolygon].decode(value).right.toOption
  }
}

case class SodaPhoto(value: String) extends SodaValue {
  type ValueType = String
  def sodaType = SodaPhoto
  def asJson = JString(value)
}

case object SodaPhoto extends SodaType with (String => SodaPhoto) {

  def convertFrom(value: JValue) = {
    JsonDecode[String].decode(value).right.toOption.map(SodaPhoto)
  }
  override def toString = "SodaPhoto"
}

case class SodaDocument(@JsonKey("file_id") fileId: String,
                        @JsonKey("content_type") contentType: Option[String],
                        @JsonKey("filename") filename: Option[String]) extends SodaValue {
  type ValueType = SodaDocument
  def sodaType = SodaDocument
  def asJson: JValue = SodaDocument.jsonCodec.encode(this)
  def value: SodaDocument = this
}

case object SodaDocument extends SodaType with ((String, Option[String], Option[String]) => SodaDocument) {
  implicit val jsonCodec = AutomaticJsonCodecBuilder[SodaDocument]

  def convertFrom(value: JValue) = jsonCodec.decode(value).right.toOption

  override def toString = "SodaDocument"
}
