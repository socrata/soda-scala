package com.socrata.soda2.http

import scala.io.Codec

import com.rojoma.json.ast.JValue

import com.socrata.http.BodyConsumer
import com.socrata.iteratee.{IdentityIteratee, CharJValueEnumeratee, ByteCharEnumeratee, ByteIteratee}

/** A bodyConsumer which expects to read a single [[com.rojoma.json.ast.JValue]] out of an
 * HTTP response. */
class SingleJValueBodyConsumer(iteratee: ByteIteratee[JValue]) extends BodyConsumer[JValue] {
  /** @param codec A codec for the body's content encoding */
  def this(codec: Codec) = this(new ByteCharEnumeratee(codec, new CharJValueEnumeratee(new IdentityIteratee)))

  def apply(bytes: Array[Byte], isLast: Boolean): Either[BodyConsumer[JValue], JValue] = {
    iteratee.process(bytes) match {
      case Right(jValue) =>
        Right(jValue)
      case Left(newIteratee) =>
        if(isLast) {
          Right(newIteratee.endOfInput())
        } else {
          Left(new SingleJValueBodyConsumer(newIteratee))
        }
    }
  }
}
