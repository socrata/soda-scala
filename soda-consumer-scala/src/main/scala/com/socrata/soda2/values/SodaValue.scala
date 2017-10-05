package com.socrata.soda2.values

import java.net.URI

import com.rojoma.json.ast.{JArray, JString, _}
import com.rojoma.json.codec.JsonCodec
import com.rojoma.json.matcher.{PObject, POption, _}
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, LocalDateTime}

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
  def convertFrom(value: JValue) = JsonCodec[String].decode(value).map(SodaString)
  override def toString = "SodaString"
}

case class SodaBlob(uri: URI) extends SodaValue {
  type ValueType = URI
  def sodaType = SodaBlob
  def asJson = JString(uri.toString)
  val value = uri
}

case object SodaBlob extends SodaType with (URI => SodaBlob) {
  def convertFrom(value: JValue) = JsonCodec[String].decode(value).map { linkStr =>
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
  def convertFrom(value: JValue) = JsonCodec[String].decode(value).map { linkStr =>
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
  def convertFrom(value: JValue) = JsonCodec[String].decode(value).map { numStr =>
    new BigDecimal(new java.math.BigDecimal(numStr)) // TODO: Catch exceptions
  }.orElse {
    JsonCodec[BigDecimal].decode(value)
  }.map(SodaNumber)

  override def toString = "SodaNumber"
}

case class SodaDouble(value: Double) extends SodaValue {
  type ValueType = Double
  def sodaType = SodaDouble
  def asJson = JNumber(value)
}

case object SodaDouble extends SodaType with (Double => SodaDouble) {
  def convertFrom(value: JValue) = JsonCodec[Double].decode(value).map(SodaDouble)
  override def toString = "SodaDouble"
}

case class SodaMoney(value: BigDecimal) extends SodaValue {
  type ValueType = BigDecimal
  def sodaType = SodaMoney
  def asJson = JString(value.toString) // FIXME: is this right?  It's certainly how we receive it!
}

case object SodaMoney extends SodaType with (BigDecimal => SodaMoney) {
  def convertFrom(value: JValue) = JsonCodec[String].decode(value).map { numStr =>
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

  implicit val jsonCodec = new JsonCodec[SodaLocation] {
    def encode(x: SodaLocation) =
      Pattern.generate(address :=? x.address, city :=? x.city, state :=? x.state, zip :=? x.zip,
        latitude :=? x.coordinates.map(_._1),
        longitude :=? x.coordinates.map(_._2))

    def decode(v: JValue) = Pattern.matches(v).map { results =>
      val coords = for {
        lat <- latitude.get(results)
        lon <- longitude.get(results)
      } yield (lat, lon)

      SodaLocation(address.get(results), city.get(results), state.get(results), zip.get(results), coords)
    }
  }

  def convertFrom(value: JValue) = JsonCodec[SodaLocation].decode(value)

  override def toString = "SodaLocation"
}

case class SodaPoint(coordinates: Option[(Double, Double)]) extends SodaValue {
  type ValueType = SodaPoint
  def sodaType = SodaPoint
  def asJson = SodaPoint.jsonCodec.encode(this)
  def value = this
}

case object SodaPoint extends SodaType with ((Option[(Double, Double)]) => SodaPoint) {
  private val longitude = Variable[Double]
  private val latitude = Variable[Double]

  private val pattern = PObject(
    "type" -> JString("Point"),
    "coordinates" -> PArray(
      POption(longitude).orNull.subPattern,
      POption(latitude).orNull.subPattern
    )
  )

  implicit val jsonCodec = new JsonCodec[SodaPoint] {
    def encode(x: SodaPoint) =
      pattern.generate(longitude :=? x.coordinates.map(_._1), latitude :=? x.coordinates.map(_._2))

    def decode(v: JValue) = pattern.matches(v).map { results =>
      val coords = for {
        lon <- longitude.get(results)
        lat <- latitude.get(results)
      } yield (lon, lat)

      SodaPoint(coords)
    }
  }

  def convertFrom(value: JValue) = JsonCodec[SodaPoint].decode(value)

  override def toString = "SodaPoint"
}

case class SodaBoolean(value: Boolean) extends SodaValue {
  type ValueType = Boolean
  def sodaType = SodaBoolean
  def asJson = JBoolean(value)
}

case object SodaBoolean extends SodaType with (Boolean => SodaBoolean) {
  def convertFrom(value: JValue) = JsonCodec[Boolean].decode(value).map(SodaBoolean)
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
    str <- JsonCodec[String].decode(value)
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
    str <- JsonCodec[String].decode(value)
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
  private[values] val simplePoint = new JsonCodec[SodaPoint] {
    private val latitude = Variable[Double]
    private val longitude = Variable[Double]
    private val Pattern = PArray(longitude, latitude)

    def encode(x: SodaPoint) =
      Pattern.generate(latitude := x.latitude,
                       longitude := x.longitude)


    def decode(v: JValue) = Pattern.matches(v) map { results =>
      SodaPoint(latitude(results), longitude(results))
    }
  }

  implicit val jsonCodec = new JsonCodec[SodaPoint] {
    private val point = Variable[SodaPoint]()(simplePoint)
    private val Pattern = PObject(
      "type" -> "Point",
      "coordinates" -> point
    )

    def encode(x: SodaPoint) = Pattern.generate(point := x)

    def decode(v: JValue) = Pattern.matches(v) map { results =>
      point(results)
    }
  }

  def convertFrom(value: JValue) = JsonCodec[SodaPoint].decode(value)
}

case class SodaMultiPoint(points: Seq[SodaPoint]) extends SodaValue {
  type ValueType = SodaMultiPoint
  def asJson = SodaMultiPoint.jsonCodec.encode(this)
  def sodaType = SodaMultiPoint
  def value = this
}

case object SodaMultiPoint extends SodaType with (Seq[SodaPoint] => SodaMultiPoint) {
  implicit val jsonCodec = new JsonCodec[SodaMultiPoint] {
    val points = Variable[Seq[SodaPoint]]()(JsonCodec.seqCodec(SodaPoint.simplePoint, implicitly))
    private val Pattern = PObject(
      "type" -> "MultiPoint",
      "coordinates" -> points
    )

    def encode(x: SodaMultiPoint) = Pattern.generate(points := x.points)

    def decode(v: JValue) = Pattern.matches(v) map { results =>
      SodaMultiPoint(points(results))
    }
  }

  def convertFrom(value: JValue) = JsonCodec[SodaMultiPoint].decode(value)
}

case class SodaLineString(points: Seq[SodaPoint]) extends SodaValue {
  type ValueType = SodaLineString
  def asJson = SodaLineString.jsonCodec.encode(this)
  def sodaType = SodaLineString
  def value = this
}

case object SodaLineString extends SodaType with (Seq[SodaPoint] => SodaLineString) {
  private[values] val simpleLineString = new JsonCodec[SodaLineString] {
    private val base = JsonCodec.seqCodec[SodaPoint, Seq](SodaPoint.simplePoint, implicitly)
    def encode(x: SodaLineString) = base.encode(x.points)
    def decode(v: JValue) = base.decode(v).map(SodaLineString)
  }

  implicit val jsonCodec = new JsonCodec[SodaLineString] {
    private val points = Variable[SodaLineString]()(simpleLineString)
    private val Pattern = PObject(
      "type" -> "LineString",
      "coordinates" -> points
    )

    def encode(x: SodaLineString) = Pattern.generate(points := x)

    def decode(v: JValue) = Pattern.matches(v) map { results =>
      points(results)
    }
  }

  def convertFrom(value: JValue) = JsonCodec[SodaLineString].decode(value)
}

case class SodaMultiLineString(lines: Seq[SodaLineString]) extends SodaValue {
  type ValueType = SodaMultiLineString
  def asJson = SodaMultiLineString.jsonCodec.encode(this)
  def sodaType = SodaMultiLineString
  def value = this
}

case object SodaMultiLineString extends SodaType with (Seq[SodaLineString] => SodaMultiLineString) {
  implicit val jsonCodec = new JsonCodec[SodaMultiLineString] {
    private val lines = Variable[Seq[SodaLineString]]()(JsonCodec.seqCodec(SodaLineString.simpleLineString, implicitly))
    private val Pattern = PObject(
      "type" -> "MultiLineString",
      "coordinates" -> lines
    )

    def encode(x: SodaMultiLineString) = Pattern.generate(lines := x.lines)

    def decode(v: JValue) = Pattern.matches(v) map { results =>
      SodaMultiLineString(lines(results))
    }
  }

  def convertFrom(value: JValue) = JsonCodec[SodaMultiLineString].decode(value)
}

case class SodaPolygon(ring: Seq[SodaPoint], holes: Seq[Seq[SodaPoint]]) extends SodaValue {
  type ValueType = SodaPolygon
  def asJson = SodaPolygon.jsonCodec.encode(this)
  def sodaType = SodaPolygon
  def value = this
}

case object SodaPolygon extends SodaType with ((Seq[SodaPoint], Seq[Seq[SodaPoint]]) => SodaPolygon) {
  private[values] val simplePolygon = new JsonCodec[SodaPolygon] {
    private val ring = JsonCodec.seqCodec[SodaPoint, Seq](SodaPoint.simplePoint, implicitly)
    private val holes = JsonCodec.seqCodec[Seq[SodaPoint], Seq](ring, implicitly)
    def encode(x: SodaPolygon) = {
      val r = ring.encode(x.ring)
      val JArray(hs) = holes.encode(x.holes)
      JArray(r +: hs)
    }
    def decode(v: JValue) = v match {
      case JArray(items) if items.nonEmpty =>
        val jr = items.head
        val jhs = items.tail
        for {
          r <- ring.decode(jr)
          hs <- holes.decode(JArray(jhs))
        } yield SodaPolygon(r, hs)
      case _ =>
        None
    }
  }

  implicit val jsonCodec = new JsonCodec[SodaPolygon] {
    val points = Variable[SodaPolygon]()(simplePolygon)
    private val Pattern = PObject(
      "type" -> "Polygon",
      "coordinates" -> points
    )

    def encode(x: SodaPolygon) = Pattern.generate(points := x)

    def decode(v: JValue) = Pattern.matches(v) map { results =>
      points(results)
    }
  }

  def convertFrom(value: JValue) = JsonCodec[SodaPolygon].decode(value)
}

case class SodaMultiPolygon(polygons: Seq[SodaPolygon]) extends SodaValue {
  type ValueType = SodaMultiPolygon
  def asJson = SodaMultiPolygon.jsonCodec.encode(this)
  def sodaType = SodaMultiPolygon
  def value = this
}

case object SodaMultiPolygon extends SodaType with (Seq[SodaPolygon] => SodaMultiPolygon) {
  implicit val jsonCodec = new JsonCodec[SodaMultiPolygon] {
    private val polygons = Variable[Seq[SodaPolygon]]()(JsonCodec.seqCodec(SodaPolygon.simplePolygon, implicitly))
    private val Pattern = PObject(
      "type" -> "MultiPolygon",
      "coordinates" -> polygons
    )

    def encode(x: SodaMultiPolygon) = Pattern.generate(polygons := x.polygons)

    def decode(v: JValue) = Pattern.matches(v) map { results =>
      SodaMultiPolygon(polygons(results))
    }
  }

  def convertFrom(value: JValue) = JsonCodec[SodaMultiPolygon].decode(value)
}
