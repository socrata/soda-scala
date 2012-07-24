package com.socrata
package iteratee

import scala.annotation.tailrec

import com.rojoma.json
import json.ast._
import json.util.{JValueProducer, WrappedCharArray}

class CharJValueEnumeratee[T](valueProducer: JValueProducer, iteratee: Iteratee[JValue, T]) extends CharIteratee[T] {
  def this(iteratee: Iteratee[JValue, T]) = this(JValueProducer.newProducer, iteratee)

  def process(input: WrappedCharArray): Either[CharIteratee[T], T] = {
    @tailrec
    def loop(valueProducer: JValueProducer, input: WrappedCharArray, iteratee: Iteratee[JValue, T]): Either[CharIteratee[T], T] = {
      valueProducer.consume(input) match {
        case JValueProducer.Value(v, newProducer, remainingInput) =>
          iteratee.process(v) match {
            case Right(result) => Right(result)
            case Left(newIteratee) => loop(JValueProducer.newProducerFromLexer(newProducer), remainingInput, newIteratee)
          }
        case JValueProducer.More(newProducer) =>
          Left(new CharJValueEnumeratee(newProducer, iteratee))
      }
    }
    loop(valueProducer, input, iteratee)
  }

  def endOfInput(): T = {
    valueProducer.finish() match {
      case JValueProducer.FinalValue(v, _, _) =>
        iteratee.process(v) match {
          case Right(result) => result
          case Left(newIteratee) => newIteratee.endOfInput()
        }
    }
  }
}

