package com.socrata.soda2.publisher

import com.rojoma.json.v3.util.{SimpleJsonCodecBuilder, SimpleHierarchyCodecBuilder, NoTag}

sealed abstract class UpsertResponse {
  def created: Int
  def updated: Int
  def deleted: Int
}
case class UpsertResponseLegacy(created: Int, updated: Int, deleted: Int, byIdentifier: Int, errors: Int, bySid: Int) extends UpsertResponse
case class UpsertResponse2(created: Int, updated: Int, deleted: Int) extends UpsertResponse

object UpsertResponse {
  implicit val jCodecLegacy = SimpleJsonCodecBuilder[UpsertResponseLegacy].build(
    "Rows Created", _.created,
    "Rows Updated", _.updated,
    "Rows Deleted", _.deleted,
    "By RowIdentifier", _.byIdentifier,
    "Errors", _.errors,
    "By SID", _.bySid)

  implicit val jCodec2 = SimpleJsonCodecBuilder[UpsertResponse2].build(
    "rows_created", _.created,
    "rows_updated", _.updated,
    "rows_deleted", _.deleted)

  implicit val jCodec = SimpleHierarchyCodecBuilder[UpsertResponse](NoTag).
    branch[UpsertResponseLegacy].
    branch[UpsertResponse2].
    build
}
