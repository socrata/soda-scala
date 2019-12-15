package com.socrata.soda2.publisher

import scala.concurrent.Future

import com.rojoma.json.v3.ast._

import com.socrata.soda2.consumer.{JValueRowEnumeratee, Row, Consumer}
import com.socrata.soda2.values.SodaValue
import com.socrata.soda2.{Resource, ColumnNameLike, MalformedResponseJsonException, ResourceLike}
import com.socrata.iteratee.{IdentityIteratee, CharJValueEnumeratee}
import com.socrata.soda2.consumer.impl.QueryRunner

trait Publisher extends Consumer {
  import Publisher._

  def upsert[R, C](resource: R, additions: Seq[Map[C, SodaValue]] = Nil, deletions: TraversableOnce[Long] = Nil)(implicit ev : ResourceLike[R], ev2: ColumnNameLike[C]): Future[UpsertResponse] =
    lowLevel.postJson(
      ev.asResource(resource),
      JArray(additions.map(toAdditionObject[C]) ++ deletions.toSeq.map(toDeleteObject)),
      { (_, _) =>
        new CharJValueEnumeratee(
          impl.UpsertResponseIteratee,
          { e => throw new MalformedResponseJsonException("Malformed JSON encountered while reading upsert response", e) }) })

  def upsertSingle[R, C](resource: R, addition: Map[C, SodaValue])(implicit ev : ResourceLike[R], ev2: ColumnNameLike[C]): Future[Row] =
    lowLevel.postJson(
      ev.asResource(resource),
      toAdditionObject(addition),
      { (uri, metadata) =>
        new CharJValueEnumeratee(
          new JValueRowEnumeratee(QueryRunner.rowDecoderFor(uri, metadata), new IdentityIteratee),
          { e => throw new MalformedResponseJsonException("Malformed JSON encountered while reading upsert response", e) }) })

  def makeWorkingCopy[R](resource: R, copyRows: Boolean = true)(implicit ev: ResourceLike[R]): Future[Resource] =
    lowLevel.legacyMakeWorkingCopy(
      ev.asResource(resource),
      copyRows,
      { (_, _) =>
        new CharJValueEnumeratee(
        new impl.MakeWorkingCopyResponseIteratee("copy"),
        { e => throw new MalformedResponseJsonException("Malformed JSON encountered while reading copy response", e) }) })

  def publish[R](resource: R)(implicit ev: ResourceLike[R]): Future[Resource] =
    lowLevel.legacyPublish(
      ev.asResource(resource),
      { (_, _) =>
        new CharJValueEnumeratee(
        new impl.MakeWorkingCopyResponseIteratee("publish"),
        { e => throw new MalformedResponseJsonException("Malformed JSON encountered while reading publish response", e) }) })
}

object Publisher {
  private val deletedKV = ":deleted" -> JBoolean(true)

  private def toDeleteObject(sid: Long) = JObject(Map(
    deletedKV,
    ":id" -> JNumber(sid)
  ))

  private def toAdditionObject[C](row: Map[C, SodaValue])(implicit ev: ColumnNameLike[C]): JObject = {
    val resultingRow = row.foldLeft(Map.empty[String, JValue]) { (rowsSoFar, colVal) =>
      val (col, value) = colVal
      rowsSoFar + (ev.asColumnName(col).toString -> value.asJson)
    }
    JObject(resultingRow)
  }
}
