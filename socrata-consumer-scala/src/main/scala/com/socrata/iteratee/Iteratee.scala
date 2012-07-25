package com.socrata.iteratee

trait Iteratee[-I, +O] {
  def process(input: I): Either[Iteratee[I, O], O]
  def endOfInput(): O
}
