package com.socrata.soda2.consumer.impl

import java.net.URI

import com.rojoma.json.ast._
import com.socrata.soda2.ColumnName
import org.scalatest.{MustMatchers, WordSpec}

class RowDecoderTest extends WordSpec with MustMatchers {

  val datasetBase = new URI("https://example.com")
  val rawSchema = Map(
    ":created_at" -> "fixed_timestamp",
    ":id"         -> "row_identifier",
    ":updated_at" -> "fixed_timestamp",
    ":version"    -> "row_version",
    "date"        -> "floating_timestamp",
    "rate"        -> "number",
    "empty"       -> "number"
  ).map { case (k, v) => ColumnName(k) -> v }

  val decoder = new RowDecoder(datasetBase, rawSchema)
  val row =
    JObject(Map(
      ":created_at"     -> JString("2015-10-13T08:33:34.969Z"),
      ":id"             -> JString("row-tzm7~78fh_xk9g"),
      ":updated_at"     -> JString("2015-10-13T08:33:34.969Z"),
      ":version"        -> JString("rv-2tab~38pv-z27z"),
      "date"            -> JString("2013-06-30T00:00:00.000"),
      "rate"            -> JString("0.47"),
      "irrelevant_col1" -> JString("another value")
    ))

  "RowDecoder" should {
    lazy val fields = decoder(row).asMap.map { case (k, v) => k.toString -> v }

    "have convert json to row without any exception" in {
      noException must be thrownBy fields
    }

    "have relevant fields only" in {
      fields.keys must contain allOf(":created_at", ":id", ":updated_at", ":version", "date", "rate")
    }

    "strip down irrelevant fields" in {
      fields must not contain key("irrelevant_col1")
    }

    "strip down null fields" in {
      fields.get("empty") must equal(Some(None))
    }

  }

}
