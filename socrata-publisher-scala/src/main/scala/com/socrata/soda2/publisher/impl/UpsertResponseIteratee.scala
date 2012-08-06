package com.socrata.soda2.publisher
package impl

import com.rojoma.json.ast.JValue
import com.rojoma.json.codec.JsonCodec

import com.socrata.iteratee.Iteratee
import com.socrata.soda2.{InvalidResponseJsonException, MalformedResponseJsonException}

object UpsertResponseIteratee extends Iteratee[JValue, UpsertResponse] {
  def process(input: JValue) =
    Right(JsonCodec[UpsertResponse].decode(input).getOrElse {
      throw new InvalidResponseJsonException(input, "Unable to interpret response as an upsert response")
    })

  def endOfInput() =
    throw new MalformedResponseJsonException("End of input while awaiting upsert response")
}
