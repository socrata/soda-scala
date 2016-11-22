package com.socrata.soda2.consumer
package impl

import org.scalatest.MustMatchers
import org.scalatest.WordSpec
import com.rojoma.json.util.JsonUtil

import com.socrata.soda2.ColumnName

class QueryRunnerTest extends WordSpec with MustMatchers {
  def r(names: String*) = JsonUtil.renderJson(names)

  def rawSchema(fields: Seq[String], types: Seq[String]) = Map(
    "fields" -> r(fields: _*),
    "types" -> r(types: _*)
  )

  "extractRawSchema" should {
    "fail if there are more fields than types" in {
      an [InvalidMetadataValues] must be thrownBy {
        QueryRunner.extractRawSchema(rawSchema(List("a","b","c"), List("one","two")))
      }
    }

    "fail if there are fewer fields than types" in {
      an [InvalidMetadataValues] must be thrownBy {
        QueryRunner.extractRawSchema(rawSchema(List("a","b"), List("one","two","three")))
      }
    }

    "fail if there is no \"fields\" field" in {
      a [MissingMetadataField] must be thrownBy {
        QueryRunner.extractRawSchema(Map("types" -> r("one","two","three")))
      }
    }

    "fail if there is no \"types\" field" in {
      a [MissingMetadataField] must be thrownBy {
        QueryRunner.extractRawSchema(Map("fields" -> r("a","b","c")))
      }
    }

    "fail if \"fields\" is not valid JSON" in {
      a [MalformedMetadataField] must be thrownBy {
        QueryRunner.extractRawSchema(Map("fields" -> "%#@$^#%$^#@%$", "types" -> "[]"))
      }
    }

    "fail if \"types\" is not valid JSON" in {
      a [MalformedMetadataField] must be thrownBy {
        QueryRunner.extractRawSchema(Map("fields" -> "[]", "types" -> "%#@$^#%$^#@%$"))
      }
    }

    "fail if \"fields\" is not a list of strings" in {
      a [MalformedMetadataField] must be thrownBy {
        QueryRunner.extractRawSchema(Map("fields" -> "[1,2,3]", "types" -> r("a","b","c")))
      }
    }

    "fail if \"types\" is not a list of strings" in {
      a [MalformedMetadataField] must be thrownBy {
        QueryRunner.extractRawSchema(Map("fields" -> r("a","b","c"), "types" -> "%#@$^#%$^#@%$"))
      }
    }

    "produce a map of fields and types if there are the same number" in {
      QueryRunner.extractRawSchema(rawSchema(List("a","b","c"), List("one","two", "three"))) must equal (
        Map(
          ColumnName("a") -> "one",
          ColumnName("b") -> "two",
          ColumnName("c") -> "three")
      )
    }
  }
}
