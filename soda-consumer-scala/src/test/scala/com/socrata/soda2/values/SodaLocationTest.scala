package com.socrata.soda2.values

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

  "SodaLocation" should {

    "encode and decode back any SodaLocation object" in {
      forAll { obj: SodaLocation =>
        obj must equal (SodaLocation.convertFrom(obj.asJson).value)
      }
    }

  }

}
