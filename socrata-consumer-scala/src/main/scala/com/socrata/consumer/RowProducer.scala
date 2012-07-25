package com.socrata.consumer

import scala.io.Codec

import com.socrata.http.BodyConsumer
import com.socrata.iteratee.{ByteCharEnumeratee, ByteIteratee, CharIteratee}

private[consumer] class RowProducer[T](iteratee: ByteIteratee[T]) extends BodyConsumer[T] {
  def this(codec: Codec, iteratee: CharIteratee[T]) = this(new ByteCharEnumeratee(codec, iteratee))

  def apply(bytes: Array[Byte], isLast: Boolean): Either[BodyConsumer[T], T] = {
    iteratee.process(bytes) match {
      case Right(t) => Right(t)
      case Left(newByteIteratee) =>
        if (isLast) Right(newByteIteratee.endOfInput())
        else Left(new RowProducer(newByteIteratee))
    }
  }
}
