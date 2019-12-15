package com.socrata.soda2.http

import scala.io.Codec

import com.rojoma.json.v3.ast.JValue
import com.rojoma.json.v3.io.JsonReaderException

import com.socrata.http.BodyConsumer
import com.socrata.iteratee.{JValueIteratee, ByteCharEnumeratee, ByteIteratee}
import com.socrata.soda2.MalformedResponseJsonException

/** A bodyConsumer which expects to read a single [[com.rojoma.json.v3.ast.JValue]] out of an
 * HTTP response. */
class SingleJValueBodyConsumer(iteratee: ByteIteratee[JValue]) extends BodyConsumer[JValue] {
  /** @param codec A codec for the body's content encoding */
  def this(codec: Codec) = this(new ByteCharEnumeratee(codec, new JValueIteratee(SingleJValueBodyConsumer.decodeError)))

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

object SingleJValueBodyConsumer {
  private def decodeError(ex: JsonReaderException) =
    throw new MalformedResponseJsonException("Malformed JSON encountered while reading response object", ex)
}
