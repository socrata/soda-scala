package com.socrata.soda2.values

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

import com.rojoma.json.v3.io.JsonReader

class SodaGeoTest extends WordSpec with MustMatchers with GeneratorDrivenPropertyChecks with OptionValues {
  implicit val arbitraryPoint: Arbitrary[SodaPoint] = Arbitrary {
    for {
      lat <- arbitrary[Double]
      lon <- arbitrary[Double]
    } yield SodaPoint(lon, lat)
  }

  implicit val arbitraryMultiPoint: Arbitrary[SodaMultiPoint] = Arbitrary {
    for {
      pts <- arbitrary[Seq[SodaPoint]]
    } yield SodaMultiPoint(pts)
  }

  implicit val arbitraryLineString: Arbitrary[SodaLineString] = Arbitrary {
    for {
      pts <- arbitrary[Seq[SodaPoint]]
    } yield SodaLineString(pts)
  }

  implicit val arbitraryMultiLineString: Arbitrary[SodaMultiLineString] = Arbitrary {
    for {
      pts <- arbitrary[Seq[SodaLineString]]
    } yield SodaMultiLineString(pts)
  }

  implicit val arbitraryPolygon: Arbitrary[SodaPolygon] = Arbitrary {
    for {
      ring <- arbitrary[Seq[SodaPoint]]
      holes <- arbitrary[Seq[Seq[SodaPoint]]]
    } yield SodaPolygon(ring, holes)
  }

  implicit val arbitraryMultiPolygon: Arbitrary[SodaMultiPolygon] = Arbitrary {
    for {
      pts <- arbitrary[Seq[SodaPolygon]]
    } yield SodaMultiPolygon(pts)
  }

  def lit(s: String) = JsonReader.fromString(s)

  def rfc(name: String, typ: SodaType, s: String, value: SodaValue) = {
    ("decode the rfc7946 " + name + " example") in {
      typ.convertFrom(JsonReader.fromString(s)) must equal (Some(value))
    }
  }

  "SodaPoint" should {
    "round-trip" in {
      forAll { obj: SodaPoint =>
        obj must equal (SodaPoint.convertFrom(obj.asJson).value)
      }
    }

    rfc("point",
        SodaPoint,
        """{ "type": "Point", "coordinates": [100.0, 0.0] }""",
        SodaPoint(0, 100))
  }

  "SodaMultiPoint" should {
    "round-trip" in {
      forAll { obj: SodaMultiPoint =>
        obj must equal (SodaMultiPoint.convertFrom(obj.asJson).value)
      }
    }

    rfc("multipoint",
        SodaMultiPoint,
        """{ "type": "MultiPoint", "coordinates": [ [100.0, 0.0], [101.0, 1.0] ] }""",
        SodaMultiPoint(Seq(SodaPoint(0, 100), SodaPoint(1, 101))))
  }

  "SodaLineString" should {
    "round-trip" in {
      forAll { obj: SodaLineString =>
        obj must equal (SodaLineString.convertFrom(obj.asJson).value)
      }
    }

    rfc("linestring",
        SodaLineString,
        """{ "type": "LineString", "coordinates": [ [100.0, 0.0], [101.0, 1.0] ] }""",
        SodaLineString(Seq(SodaPoint(0, 100), SodaPoint(1, 101))))
  }

  "SodaMultiLineString" should {
    "round-trip" in {
      forAll { obj: SodaMultiLineString =>
        obj must equal (SodaMultiLineString.convertFrom(obj.asJson).value)
      }
    }

    rfc("multilinestring",
        SodaMultiLineString,
        """{ "type": "MultiLineString", "coordinates": [ [ [100.0, 0.0], [101.0, 1.0] ],
                                                         [ [102.0, 2.0], [103.0, 3.0] ] ] }""",
        SodaMultiLineString(Seq(SodaLineString(Seq(SodaPoint(0, 100), SodaPoint(1, 101))),
                                SodaLineString(Seq(SodaPoint(2, 102), SodaPoint(3, 103))))))
  }

  "SodaPolygon" should {
    "round-trip" in {
      forAll { obj: SodaPolygon =>
        obj must equal (SodaPolygon.convertFrom(obj.asJson).value)
      }
    }

    rfc("polygon",
        SodaPolygon,
        """{ "type": "Polygon", "coordinates": [ [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ],
                                                 [ [100.8, 0.8], [100.8, 0.2], [100.2, 0.2], [100.2, 0.8], [100.8, 0.8] ] ] }""",
        SodaPolygon(Seq(SodaPoint(0, 100), SodaPoint(0, 101), SodaPoint(1, 101), SodaPoint(1, 100), SodaPoint(0, 100)),
                    Seq(Seq(SodaPoint(0.8, 100.8), SodaPoint(0.2, 100.8), SodaPoint(0.2, 100.2), SodaPoint(0.8, 100.2), SodaPoint(0.8, 100.8)))))
  }

  "SodaMultiPolygon" should {
    "round-trip" in {
      forAll { obj: SodaMultiPolygon =>
        obj must equal (SodaMultiPolygon.convertFrom(obj.asJson).value)
      }
    }

    rfc("multipolygon",
        SodaMultiPolygon,
        """{ "type": "MultiPolygon", "coordinates": [ [ [ [102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0], [102.0, 2.0] ] ],
                                                      [ [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ],
                                                        [ [100.2, 0.2], [100.2, 0.8], [100.8, 0.8], [100.8, 0.2], [100.2, 0.2] ] ] ] }""",
        SodaMultiPolygon(Seq(SodaPolygon(Seq(SodaPoint(2, 102), SodaPoint(2, 103), SodaPoint(3, 103), SodaPoint(3, 102), SodaPoint(2, 102)), Nil),
                             SodaPolygon(Seq(SodaPoint(0, 100), SodaPoint(0, 101), SodaPoint(1, 101), SodaPoint(1, 100), SodaPoint(0, 100)),
                                         Seq(Seq(SodaPoint(0.2, 100.2), SodaPoint(0.8, 100.2), SodaPoint(0.8, 100.8), SodaPoint(0.2, 100.8), SodaPoint(0.2, 100.2)))))))
  }
}

