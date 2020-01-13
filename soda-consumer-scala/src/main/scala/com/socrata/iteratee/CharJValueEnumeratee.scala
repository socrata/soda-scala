package com.socrata.iteratee

import scala.annotation.tailrec
import com.rojoma.json.v3.ast._
import com.rojoma.json.v3.io.{JsonEventGenerator, JsonReaderException, JsonTokenGenerator}
import com.rojoma.json.v3.util
import com.rojoma.json.v3.util.{JValueProducer, WrappedCharArray}

/** An [[com.socrata.iteratee.Iteratee]] (actually an Enumeratee) that reads a JSON data out of the stream of
 * [[com.rojoma.json.v3.util.WrappedCharArray]]s which it consumes, and passes them on to a secondary
 * [[com.socrata.iteratee.Iteratee]] for further processing, throwing exceptions if the data is not well-formed.
 */
class CharJValueEnumeratee[T](valueProducer: JValueProducer, iteratee: Iteratee[JValue, T], onDecodeError: JsonReaderException => T) extends CharIteratee[T] {
  /** Constructs a new `CharJValueEnumeratee` with the given secondary [[com.socrata.iteratee.Iteratee]]. */
  def this(iteratee: Iteratee[JValue, T], onDecodeError: JsonReaderException => T) = this(new util.JValueProducer.Builder().build, iteratee, onDecodeError)
  def this(iteratee: Iteratee[JValue, T]) = this(new util.JValueProducer.Builder().build, iteratee, CharJValueEnumeratee.defaultDecodeErrorAction)

  private def handleError[A](action: => A): Either[A, T] =
    try {
      Left(action)
    } catch {
      case e: JsonReaderException =>
        Right(onDecodeError(e))
    }

  def process(input: WrappedCharArray): Either[CharIteratee[T], T] = {
    @tailrec
    def loop(valueProducer: JValueProducer, input: WrappedCharArray, iteratee: Iteratee[JValue, T]): Either[CharIteratee[T], T] = {
      handleError(valueProducer.consume(input)) match {
        case Right(result) => Right(result)
        case Left(x) => x match {
          case JValueProducer.Value(v, newProducer, remainingInput) =>
            iteratee.process(v) match {
              case Right(result) => Right(result)
              case Left(newIteratee) => loop(new JValueProducer.Builder().withLexer(newProducer).build, remainingInput, newIteratee)
            }
          case JValueProducer.More(newProducer) =>
            Left(new CharJValueEnumeratee(newProducer, iteratee, onDecodeError))
        }
      }
    }
    loop(valueProducer, input, iteratee)
  }

  def endOfInput(): T = {
    handleError(valueProducer.finish()) match {
      case Right(x) => x
      case Left(x) => x match {
        case JValueProducer.FinalValue(v, _) =>
          iteratee.process(v) match {
            case Right(result) => result
            case Left(newIteratee) => newIteratee.endOfInput()
          }
      }
    }
  }
}

object CharJValueEnumeratee {
  private def defaultDecodeErrorAction(e: JsonReaderException) = throw e
}
