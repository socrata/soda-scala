package com.socrata.soda2.consumer
package impl

import com.socrata.soda2.consumer.LowLevel
import com.socrata.future.Future
import com.socrata.iteratee._

abstract class QueryRunner(lowLevel: LowLevel) {
  protected def executeQuery[T](iteratee: CharIteratee[T]): Future[T]

  /** Feeds [[com.socrata.soda2.consumer.Row]] objects into an [[com.socrata.iteratee.Iteratee]] to
   * produce a result.  This is the most general data access method. */
  def iterate[T](iteratee: Iteratee[Row, T]): Future[T] =
    executeQuery(new CharJArrayElementEnumeratee(
      new JValueRowEnumeratee(iteratee),
      { e => throw new MalformedJsonWhileReadingRowsException(e) }))

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
