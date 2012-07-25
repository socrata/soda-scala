package com.socrata

import scala.{collection => sc}

package object http {
  type StatusConsumer[T] = Status => Either[HeadersConsumer[T], T]
  type HeadersConsumer[T] = sc.Map[String, Seq[String]] => Either[BodyConsumer[T], T]
  trait BodyConsumer[T] extends Function2[Array[Byte], Boolean, Either[BodyConsumer[T], T]] {
    def apply(bytes: Array[Byte], isLast: Boolean): Either[BodyConsumer[T], T]
  }

  type Retryable[T] = Either[NewRequest[T], T]
}

