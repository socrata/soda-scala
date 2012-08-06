package com.socrata.soda2.publisher

import com.rojoma.json.util.SimpleJsonCodecBuilder

case class UpsertResponse(created: Int)

object UpsertResponse {
  implicit val jCodec = SimpleJsonCodecBuilder[UpsertResponse].gen("Rows Created", _.created)
}
