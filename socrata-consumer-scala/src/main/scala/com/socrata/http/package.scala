package com.socrata

import scala.{collection => sc}
import com.ning.http.client.RequestBuilderBase

package object http {
  type StatusConsumer[+T] = Status => Either[HeadersConsumer[T], T]
  type HeadersConsumer[+T] = sc.Map[String, Seq[String]] => Either[BodyConsumer[T], T]
  trait BodyConsumer[+T] extends Function2[Array[Byte], Boolean, Either[BodyConsumer[T], T]] {
    def apply(bytes: Array[Byte], isLast: Boolean): Either[BodyConsumer[T], T]

    // Hey, I'm a functor!
    def map[U](f: T => U): BodyConsumer[U] = new MappedBodyConsumer(this, f)
  }

  type Retryable[+T] = Either[NewRequest, T]

  private class MappedBodyConsumer[A, B](bc: BodyConsumer[A], f: A => B) extends BodyConsumer[B] {
    def apply(bytes: Array[Byte], isLast: Boolean) =
      bc(bytes, isLast) match {
        case Left(bc2) => Left(new MappedBodyConsumer(bc2, f))
        case Right(r) => Right(f(r))
      }
  }
}

