package com.socrata

import scala.{collection => sc}
import com.ning.http.client.RequestBuilderBase

package object http {
  /** The entry point into an HTTP processing status machine.  This is called by a [[com.socrata.http.NiceAsyncHandler]]
   * with the status line and may either transition into the consuming-headers state by returning `Left` or terminate
   * processing by either throwing an exception or returning `Right`. */
  type StatusConsumer[+T] = Status => Either[HeadersConsumer[T], T]

  /** A collection of HTTP headers */
  type Headers = sc.Map[String, Seq[String]]

  /** The header-consuming stage of an HTTP processing state machine.  This is called by a
   * [[com.socrata.http.NiceAsyncHandler]] with the complete HTTP headers, and may either transition
   * into the consuming-body state by returning `Left` or terminate processing by either throwing an
   * exception or returning `Right`. */
  type HeadersConsumer[+T] = Headers => Either[BodyConsumer[T], T]

  /** The body-consuming stage of an HTTP processing state machine.  This is called by a
   * [[com.socrata.http.NiceAsyncHandler]] with chunks of the HTTP response body.  It may either
   * continue processing by returning `Left` or terminate processing by either throwing an exception
   * or returning `Right`. */
  trait BodyConsumer[+T] extends Function2[Array[Byte], Boolean, Either[BodyConsumer[T], T]] {
    /** Process one chunk of the HTTP response body.
     *
     * @param bytes The chunk.
     * @param isLast `true` if this will not be called again.
     * @return Either the next state or the final result.
     */
    def apply(bytes: Array[Byte], isLast: Boolean): Either[BodyConsumer[T], T]

    /** @return A `BodyConsumer` which will apply a function to the final
     * result of this `BodyConsumer`.
     */
    def map[U](f: T => U): BodyConsumer[U] = new MappedBodyConsumer(this, f)
  }

  private class MappedBodyConsumer[A, B](bc: BodyConsumer[A], f: A => B) extends BodyConsumer[B] {
    def apply(bytes: Array[Byte], isLast: Boolean) =
      bc(bytes, isLast) match {
        case Left(bc2) => Left(new MappedBodyConsumer(bc2, f))
        case Right(r) => Right(f(r))
      }
  }
}

