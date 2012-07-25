package com.socrata.iteratee

import scala.annotation.tailrec

import com.rojoma.json
import json.ast._
import json.util.{JArrayProducer, WrappedCharArray}

class CharJArrayElementEnumeratee[T](arrayProducer: JArrayProducer, iteratee: Iteratee[JValue, T]) extends CharIteratee[T] {
  def this(iteratee: Iteratee[JValue, T]) = this(JArrayProducer.newProducer, iteratee)

  def process(input: WrappedCharArray): Either[CharIteratee[T], T] = {
    @tailrec
    def loop(arrayProducer: JArrayProducer, input: WrappedCharArray, iteratee: Iteratee[JValue, T]): Either[CharIteratee[T], T] = {
      arrayProducer.consume(input) match {
        case JArrayProducer.Element(v, newProducer, remainingInput) =>
          iteratee.process(v) match {
            case Right(result) => Right(result)
            case Left(newIteratee) => loop(newProducer, remainingInput, newIteratee)
          }
        case JArrayProducer.More(newProducer) =>
          Left(new CharJArrayElementEnumeratee(newProducer, iteratee))
        case JArrayProducer.EndOfList(_, _) =>
          Right(iteratee.endOfInput())
      }
    }
    loop(arrayProducer, input, iteratee)
  }

  def endOfInput(): T = {
    arrayProducer.finish() match {
      case JArrayProducer.FinalEndOfList(_, _) =>
        iteratee.endOfInput()
    }
  }
}

