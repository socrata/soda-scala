package com.socrata.iteratee

import scala.io.Codec

import java.nio.{CharBuffer, ByteBuffer}
import java.nio.charset.CoderResult
import java.nio.charset.CharsetDecoder

import com.rojoma.json.util.WrappedCharArray

/** An Enumeratee which reads character data out of a byte stream and passes them down in chunks to a
 * secondary [[com.socrata.iteratee.Iteratee]].
 */
class ByteCharEnumeratee[T](decoder: CharsetDecoder, outBuf: CharBuffer, iteratee: CharIteratee[T]) extends ByteIteratee[T] {
  /** Constructs the `ByteCharIteratee` with the given codec.  This iteratee's `process`
   * and `endOfInput` methods will throw any exceptions thrown by a decoder produced from
   * that codec.
   */
  def this(codec: Codec, iteratee: CharIteratee[T]) = this(codec.decoder, CharBuffer.allocate(4096), iteratee)

  private def toWrappedArray(buf: CharBuffer) = {
    buf.flip()
    val ca = WrappedCharArray(buf.array, buf.arrayOffset + buf.position, buf.remaining)
    buf.clear()
    ca
  }

  def process(bytes: Array[Byte]): Either[ByteIteratee[T], T] = {
    val inBuf = ByteBuffer.wrap(bytes)
    var currentIteratee = iteratee
    while(inBuf.hasRemaining) {
      decoder.decode(inBuf, outBuf, false) match {
        case CoderResult.OVERFLOW =>
          currentIteratee.process(toWrappedArray(outBuf)) match {
            case Right(t) => return Right(t)
            case Left(newIt) => currentIteratee = newIt
          }
        case CoderResult.UNDERFLOW =>
          // ok, there should be no more in the buffer
      }
    }

    currentIteratee.process(toWrappedArray(outBuf)) match {
      case Right(t) => return Right(t)
      case Left(newIt) => currentIteratee = newIt
    }

    Left(new ByteCharEnumeratee(decoder, outBuf, currentIteratee))
  }

  def endOfInput(): T = {
    val finalIt = iteratee.process(toWrappedArray(outBuf)) match {
      case Right(t) => return t
      case Left(newIt) => newIt
    }
    finalIt.endOfInput()
  }
}
