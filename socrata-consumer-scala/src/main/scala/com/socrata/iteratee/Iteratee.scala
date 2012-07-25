package com.socrata.iteratee

trait Iteratee[-I, +O] { self =>
  def process(input: I): Either[Iteratee[I, O], O]
  def endOfInput(): O

  def map[O2](f: O => O2): Iteratee[I, O2] = new Iteratee[I, O2] {
    def process(input: I) = self.process(input) match {
      case Left(i2) => Left(i2.map(f))
      case Right(i) => Right(f(i))
    }
    def endOfInput() = f(self.endOfInput())
  }
}
