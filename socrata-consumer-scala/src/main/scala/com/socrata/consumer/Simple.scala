package com.socrata.consumer

import com.socrata.future.Future
import com.socrata.iteratee._

class Simple(val lowLevel: LowLevel) {
  def query(resource: Resource, getParameters: Map[String, String]) = new SimpleQuery(lowLevel, "/id/" + resource.name, getParameters)
}

class SimpleQuery(lowLevel: LowLevel, resource: String, getParameters: Map[String, String]) {
  def iterate[T](iteratee: Iteratee[Row, T]): Future[T] =
    lowLevel.execute(resource, getParameters, new CharJArrayElementEnumeratee(new JValueRowEnumeratee(iteratee)))

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

  private class SideEffectingIteratee[U](f: Row => U) extends Iteratee[Row, Unit] {
    def process(row: Row) = { f(row); Left(this) }
    def endOfInput() {}
  }

  private class FoldingIteratee[T](seed: T, f: (T, Row) => T) extends Iteratee[Row, T] {
    def process(row: Row) = Left(new FoldingIteratee(f(seed, row), f))
    def endOfInput() = seed
  }
}
