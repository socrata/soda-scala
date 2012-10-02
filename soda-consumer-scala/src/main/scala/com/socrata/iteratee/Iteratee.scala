package com.socrata.iteratee

/** An `Iteratee` represents a computation over a stream of data.
 *
 * At each step, the `Iteratee` receives a stream element of type `I`.
 * It can then either signal that processing the stream should continue
 * by returning a `Left` containing another `Iteratee` of the same type,
 * or that processing is finished by returning a value of type `O`.
 *
 * Classical iteratees always work on infinite streams.  These, however,
 * can be used on finite ones.  At the end of the stream, if the `Iteratee`
 * has not already signalled that processing should terminate, its `endOfInput`
 * method is called to produce the final value.
 */
trait Iteratee[-I, +O] { self =>
  /** Consume one piece of input.  If the computation is not finished, a new `Iteratee`
   * is returned.
   *
   * @param input The data to consume.
   * @return Either a new `Iteratee` or the final result.
   */
  def process(input: I): Either[Iteratee[I, O], O]

  /** Signals that there will be no more data forthcoming.  The `Iteratee`
   * must now produce its final value.
   *
   * @return The output of this `Iteratee`. */
  def endOfInput(): O

  /** Produces an `Iteratee` that transforms the final result of this one.
   *
   * @tparam O2 The new final type.
   * @param f The function to apply to the final value.
   * @return A new iteratee which will eventually return a value of type `O2`.
   **/
  def map[O2](f: O => O2): Iteratee[I, O2] = new Iteratee[I, O2] {
    def process(input: I) = self.process(input) match {
      case Left(i2) => Left(i2.map(f))
      case Right(i) => Right(f(i))
    }
    def endOfInput() = f(self.endOfInput())
  }
}
