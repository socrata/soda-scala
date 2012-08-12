package com.socrata.soda2.publisher

import com.rojoma.json.util.SimpleJsonCodecBuilder

case class UpsertResponse(created: Int, updated: Int, byIdentifier: Int, deleted: Int, errors: Int, bySid: Int)

object UpsertResponse {
  implicit val jCodec = SimpleJsonCodecBuilder[UpsertResponse].gen(
    "Rows Created", _.created,
    "Rows Updated", _.updated,
    "By RowIdentifier", _.byIdentifier,
    "Rows Deleted", _.deleted,
    "Errors", _.errors,
    "By SID", _.bySid)
}
