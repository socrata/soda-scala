package com.socrata.soda2.http
package impl

import com.socrata.http.BodyConsumer

private[http] class WrappedBodyConsumer[T](underlying: BodyConsumer[T]) extends BodyConsumer[Retryable[T]] {
  def apply(bytes: Array[Byte], isLast: Boolean) =
    underlying(bytes, isLast) match {
      case Left(bc) =>
        Left(new WrappedBodyConsumer(bc))
      case Right(v) =>
        Right(Right(v))
    }
}
