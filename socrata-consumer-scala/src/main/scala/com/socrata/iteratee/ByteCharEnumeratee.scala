package com.socrata.iteratee

import scala.io.Codec

import java.nio.{CharBuffer, ByteBuffer}
import java.nio.charset.CoderResult
import java.nio.charset.CharsetDecoder

import com.rojoma.json.util.WrappedCharArray

/** An Enumeratee which reads character data out of a byte stream and passes them down in chunks to a
 * secondary [[com.socrata.iteratee.Iteratee]].
 *
 * @note Because `CharsetDecoder` is stateful, this is not a "pure" iteratee!  Once `process` or `endOfInput` have
 *       been called, this iteratee has been invalidated.  It will raise an `IllegalStateException` if re-use
 *       is attempted.
 */
class ByteCharEnumeratee[T](decoder: CharsetDecoder, leftOver: Array[Byte], outBuf: CharBuffer, iteratee: CharIteratee[T]) extends ByteIteratee[T] {
  /** Constructs the `ByteCharIteratee` with the given codec.  This iteratee's `process`
   * and `endOfInput` methods will throw any exceptions thrown by a decoder produced from
   * that codec.
   */
  def this(codec: Codec, iteratee: CharIteratee[T]) = this(codec.decoder, null, CharBuffer.allocate(4096), iteratee)

  private var used = false

  private def guardValidity() {
    if(used) throw new IllegalStateException("Impure iteratee already used")
    used = true
  }

  private def toWrappedArray(buf: CharBuffer) = {
    buf.flip()
    val ca = WrappedCharArray(buf.array, buf.arrayOffset + buf.position, buf.remaining)
    buf.clear()
    ca
  }

  def process(bytes: Array[Byte]): Either[ByteIteratee[T], T] = {
    guardValidity()

    val inBuf =
      if(leftOver == null) {
        ByteBuffer.wrap(bytes)
      } else {
        val tmp = ByteBuffer.allocate(leftOver.length + bytes.length)
        tmp.put(leftOver).put(bytes).flip()
        tmp
      }

    var currentIteratee = iteratee
    var newLeftOver: Array[Byte] = null
    while(inBuf.hasRemaining) {
      decoder.decode(inBuf, outBuf, false) match {
        case CoderResult.OVERFLOW =>
          currentIteratee.process(toWrappedArray(outBuf)) match {
            case Right(t) => return Right(t)
            case Left(newIt) => currentIteratee = newIt
          }
        case CoderResult.UNDERFLOW if !inBuf.hasRemaining =>
          // ok, done with this chunk
        case CoderResult.UNDERFLOW =>
          newLeftOver = new Array[Byte](inBuf.remaining)
          inBuf.get(newLeftOver)
      }
    }

    currentIteratee.process(toWrappedArray(outBuf)) match {
      case Right(t) => return Right(t)
      case Left(newIt) => currentIteratee = newIt
    }

    Left(new ByteCharEnumeratee(decoder, newLeftOver, outBuf, currentIteratee))
  }

  def endOfInput(): T = {
    guardValidity()

    // CharsetDecoder was designed by a crazy person.
    // * It's allowed to be stateful.  So WHY will it say "nope, you'll have to re-feed me the remaining bytes
    //   when you have more input"?
    // * There is no way, without keeping track of whether you've called decode/3 previously, to know
    //   if it's safe to invoke flush (flushing a brand-new decoder will raise IllegalStateException).

    var it = iteratee
    if(leftOver != null) {
      val inBuf = ByteBuffer.wrap(leftOver)
      decoder.decode(inBuf, outBuf, true) match {
        case CoderResult.OVERFLOW =>
          it.process(toWrappedArray(outBuf)) match {
            case Right(t) => return t
            case Left(newIt) => it = newIt
          }
        case CoderResult.UNDERFLOW =>
          assert(!inBuf.hasRemaining)
      }
    }
    while((try { decoder.flush(outBuf) } catch { case _: IllegalStateException => CoderResult.UNDERFLOW }) == CoderResult.OVERFLOW) {
      it.process(toWrappedArray(outBuf)) match {
        case Right(t) => return t
        case Left(newIt) => it = newIt
      }
    }

    it.process(toWrappedArray(outBuf)) match {
      case Right(t) => return t
      case Left(newIt) => it = newIt
    }

    it.endOfInput()
  }
}
