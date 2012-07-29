package com.socrata.soda2.consumer
package impl

import com.rojoma.json.ast.JString
import com.rojoma.json.util.JsonUtil
import com.rojoma.json.io.JsonReaderException

import com.socrata.soda2.{ColumnName, Soda2Metadata}
import com.socrata.future.Future
import com.socrata.iteratee._

abstract class QueryRunner(lowLevel: LowLevel) {
  protected def executeQuery[T](iteratee: Soda2Metadata => CharIteratee[T]): Future[T]

  /** Feeds [[com.socrata.soda2.consumer.Row]] objects into an [[com.socrata.iteratee.Iteratee]] to
   * produce a result.  This is the most general data access method. */
  def iterate[T](iteratee: Iteratee[Row, T]): Future[T] =
    executeQuery { metadata =>
      new CharJArrayElementEnumeratee(
        new JValueRowEnumeratee(extractSchema(metadata), iteratee),
        { e => throw new MalformedJsonWhileReadingRowsException(e) })
    }

  private def extractSchema(metadata: Soda2Metadata): Map[ColumnName, String] = {
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
