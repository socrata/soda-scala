package com.socrata.soda2.publisher

import com.rojoma.json.ast._

import com.socrata.soda2.consumer.Consumer
import com.socrata.soda2.values.SodaValue
import com.socrata.soda2.{Resource, ColumnNameLike, MalformedResponseJsonException, ResourceLike}
import com.socrata.iteratee.CharJValueEnumeratee
import com.socrata.future.Future

trait Publisher extends Consumer {
  import Publisher._

  def upsert[R, C](resource: R, additions: Seq[Map[C, SodaValue]] = Nil, deletions: TraversableOnce[Long] = Nil)(implicit ev : ResourceLike[R], ev2: ColumnNameLike[C]): Future[UpsertResponse] =
    lowLevel.postJson(
      ev.asResource(resource),
      JArray(additions.map(toAdditionObject[C]) ++ deletions.map(toDeleteObject)),
      { (_, _) =>
        new CharJValueEnumeratee(
          impl.UpsertResponseIteratee,
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

  def toDeleteObject(sid: Long) = JObject(Map(
    deletedKV,
    ":id" -> JNumber(sid)
  ))

  def toAdditionObject[C](row: Map[C, SodaValue])(implicit ev: ColumnNameLike[C]): JObject = {
    val resultingRow = row.foldLeft(Map.empty[String, JValue]) { (rowsSoFar, colVal) =>
      val (col, value) = colVal
      rowsSoFar + (ev.asColumnName(col).toString -> value.asJson)
    }
    JObject(resultingRow)
  }
}
