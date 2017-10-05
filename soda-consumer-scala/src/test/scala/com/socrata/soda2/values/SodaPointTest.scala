package com.socrata.soda2.values

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class SodaPointTest extends WordSpec with MustMatchers with GeneratorDrivenPropertyChecks with OptionValues {
  implicit val arbitraryPointJson: Arbitrary[SodaPoint] = Arbitrary {
    for {
      coor <- arbitrary[Option[(Double, Double)]]
    } yield SodaPoint(coor)
  }

  "SodaPoint" should {
    "encode and decode back any SodaPoint object" in {
      forAll { obj: SodaPoint =>
        obj must equal(SodaPoint.convertFrom(obj.asJson).value)
      }
    }
  }
}
