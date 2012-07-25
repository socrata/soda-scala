package com.socrata.http

import scala.io.Codec

import com.rojoma.json.ast.JValue

import com.socrata.iteratee.{IdentityIteratee, CharJValueEnumeratee, ByteCharEnumeratee, ByteIteratee}

class SingleJValueBodyConsumer(iteratee: ByteIteratee[JValue]) extends BodyConsumer[JValue] {
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
