package com.socrata.soda2.consumer.impl

import java.net.URI

import com.rojoma.json.v3.ast._
import com.socrata.soda2.ColumnName
import org.scalatest.{MustMatchers, WordSpec}

class LegacyRowDecoderTest extends WordSpec with MustMatchers {

  val datasetBase = new URI("https://example.com")
  val rawSchema = Map(
    ":id" -> "meta_data",
    "col1" -> "text",
    "col2" -> "number",
    "col3" -> "double",
    "legacy_col1" -> "dataset_link"
  ).map { case (k, v) => ColumnName(k) -> v }

  val row =
    JObject(Map(
      ":id" -> JNumber(1),
      "col1" -> JString("value1"),
      "col2" -> JNumber(100),
      "col3" -> JNull,
      "legacy_col1" -> JString("some value"),
      "irrelevant_col1" -> JString("another value")
    ))

  "sodaify" should {
    val fields = LegacyRowDecoder.sodaify(datasetBase, rawSchema)(row).fields

    "have relevant fields only" in {
      fields.keys must contain allOf(":id", "col1", "col2")
    }

    "strip down legacy fields" in {
      fields must not contain key("legacy_col1")
    }

    "strip down irrelevant fields" in {
      fields must not contain key("irrelevant_col1")
    }

    "strip down null fields" in {
      fields must not contain key("col3")
    }

  }

}
