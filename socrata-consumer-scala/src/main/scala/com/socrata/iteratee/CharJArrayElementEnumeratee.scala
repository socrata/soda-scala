package com.socrata.iteratee

import scala.annotation.tailrec

import com.rojoma.json
import json.ast._
import json.util.{JArrayProducer, WrappedCharArray}
import json.io.JsonReaderException

/** An Enumeratee which reads the elements out of a character stream representing a JSON array
 * and passes them down to a secondary [[com.socrata.iteratee.Iteratee]], throwing exceptions
 * if the data is not well-formed.
 */
class CharJArrayElementEnumeratee[T](arrayProducer: JArrayProducer, iteratee: Iteratee[JValue, T], onDecodeError: JsonReaderException => T) extends CharIteratee[T] {
  /** Constructs a new `CharJArrayElementEnumeratee` with the given secondary iterator to receive the
   * array's elements.
   *
   * @param iteratee An iteratee that will receive a stream of [[com.rojoma.json.ast.JValue]]s to produce the result. */
  def this(iteratee: Iteratee[JValue, T], onDecodeError: JsonReaderException => T) = this(JArrayProducer.newProducer, iteratee, onDecodeError)

  private def handleError[A](action: => A): Either[A, T] =
    try {
      Left(action)
    } catch {
      case e: JsonReaderException =>
        Right(onDecodeError(e))
    }

  def process(input: WrappedCharArray): Either[CharIteratee[T], T] = {
    @tailrec
    def loop(arrayProducer: JArrayProducer, input: WrappedCharArray, iteratee: Iteratee[JValue, T]): Either[CharIteratee[T], T] = {
      handleError(arrayProducer.consume(input)) match {
        case Right(result) => Right(result)
        case Left(x) => x match {
          case JArrayProducer.Element(v, newProducer, remainingInput) =>
            iteratee.process(v) match {
              case Right(result) => Right(result)
              case Left(newIteratee) => loop(newProducer, remainingInput, newIteratee)
            }
          case JArrayProducer.More(newProducer) =>
            Left(new CharJArrayElementEnumeratee(newProducer, iteratee, onDecodeError))
          case JArrayProducer.EndOfList(_, _) =>
            Right(iteratee.endOfInput())
        }
      }
    }
    loop(arrayProducer, input, iteratee)
  }

  def endOfInput(): T = {
    handleError(arrayProducer.finish()) match {
      case Right(result) => result
      case Left(x) => x match {
        case JArrayProducer.FinalEndOfList(_) =>
          iteratee.endOfInput()
      }
    }
  }
}
