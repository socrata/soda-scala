package com.socrata.soda2.values

import com.rojoma.json.ast._
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class SodaLocationTest extends WordSpec with MustMatchers with GeneratorDrivenPropertyChecks with OptionValues {

  implicit val arbitrarySodaLocation: Arbitrary[SodaLocation] = Arbitrary {
    for {
      address <- arbitrary[Option[String]]
      city <- arbitrary[Option[String]]
      state <- arbitrary[Option[String]]
      zip <- arbitrary[Option[String]]
      coordinates <- arbitrary[Option[(Double, Double)]]
    } yield SodaLocation(address, city, state, zip, coordinates)
  }

  implicit val arbitraryPointJson: Arbitrary[(Double, Double, JValue)] = Arbitrary {
    for {
      lat <- arbitrary[Double]
      lon <- arbitrary[Double]
    } yield {
      val json = JObject(
        Map(
          "type" -> JString("Point"),
          "coordinates" -> JArray(
            Seq(
              JNumber(lat),
              JNumber(lon)
            )
          )
        )
      )

      (lat, lon, json)
    }
  }

  "SodaLocation" should {

    "encode and decode back any SodaLocation object" in {
      forAll { obj: SodaLocation =>
        obj must equal (SodaLocation.convertFrom(obj.asJson).value)
      }
    }

    "decode back any SodaLocation object with point type structure" in {
      forAll { (lat: Double, lon: Double, json: JValue) =>
        val loc = SodaLocation.convertFrom(json).value

        loc.coordinates.value._1 must equal (lat)
        loc.coordinates.value._2 must equal (lon)
      }
    }

  }

}
