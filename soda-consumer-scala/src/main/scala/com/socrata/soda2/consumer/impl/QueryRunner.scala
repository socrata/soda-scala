package com.socrata.soda2.consumer
package impl

import scala.concurrent.Future

import java.net.URI

import com.rojoma.json.ast.{JObject, JString}
import com.rojoma.json.util.JsonUtil
import com.rojoma.json.io.JsonReaderException

import com.socrata.soda2.{ColumnName, Soda2Metadata}
import com.socrata.iteratee._

abstract class QueryRunner(lowLevel: LowLevel) {
  import QueryRunner._

  protected def executeQuery[T](iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T]

  /** Feeds [[com.socrata.soda2.consumer.Row]] objects into an [[com.socrata.iteratee.Iteratee]] to
   * produce a result.  This is the most general data access method. */
  def iterate[T](iteratee: Iteratee[Row, T]): Future[T] =
    executeQuery { (uri, metadata) =>
      new CharJArrayElementEnumeratee(
        new JValueRowEnumeratee(rowDecoderFor(uri, metadata), iteratee),
        { e => throw new MalformedJsonWhileReadingRowsException(e) })
    }

  /** Call a side-effecting function on each returned row.
   * @note There is no promise that any given call will be invoked on the same thread as any other call.
   */
  def foreach[U](f: Row => U): Future[Unit] = iterate(new SideEffectingIteratee(f))

  /** Fold all the rows returned into a single result.  There is no way to abort processing; if you
   * require this, use the `iterate` method instead.
   */
  def foldLeft[T](seed: T)(f: (T, Row) => T): Future[T] = iterate(new FoldingIteratee(seed, f))

  /** Return all rows as a single `IndexedSeq` */
  def allRows(): Future[IndexedSeq[Row]] = foldLeft(Vector.empty[Row])(_ :+ _)
}

private [impl] class SideEffectingIteratee[U](f: Row => U) extends Iteratee[Row, Unit] {
  def process(row: Row) = { f(row); Left(this) }
  def endOfInput() {}
}

private [impl] class FoldingIteratee[T](seed: T, f: (T, Row) => T) extends Iteratee[Row, T] {
  def process(row: Row) = Left(new FoldingIteratee(f(seed, row), f))
  def endOfInput() = seed
}

object QueryRunner {
  def rowDecoderFor(uri: URI, metadata: Soda2Metadata): JObject => Row = {
    val rawSchema = extractRawSchema(metadata)
    metadata.getOrElse("Legacy-Types", "false") match {
      case "true" => new LegacyRowDecoder(uri, rawSchema)
      case _ => new RowDecoder(uri, rawSchema)
    }
  }

  def extractRawSchema(metadata: Soda2Metadata): Map[ColumnName, String] = {
    def extractStrings(field: String): Seq[String] = {
      val jvalue = try { JsonUtil.parseJson[Seq[String]](metadata.getOrElse(field, throw new MissingMetadataField(field))) }
                   catch { case e: JsonReaderException => throw new MalformedMetadataField(field, "unable to parse as JSON", e) }
      jvalue.getOrElse(throw new MalformedMetadataField(field, "not a list of strings"))
    }
    val fields = extractStrings("fields")
    val types = extractStrings("types")
    if(fields.length != types.length)
      throw new InvalidMetadataValues(Set("fields", "types"), "not the same length")
    val fieldsColumnified = fields.map { field =>
      try { ColumnName(field) }
      catch { case e: IllegalArgumentException => throw new InvalidMetadataValues(Set("fields"), "cannot interpret " + JString(field) + " as a column name") }
    }
    fieldsColumnified.zip(types).toMap
  }
}
