package com.socrata.soda2.publisher
package impl

import com.rojoma.json.v3.ast.JValue
import com.rojoma.json.v3.codec.JsonDecode
import com.socrata.iteratee.Iteratee
import com.socrata.soda2.{InvalidResponseJsonException, MalformedResponseJsonException}

object UpsertResponseIteratee extends Iteratee[JValue, UpsertResponse] {
  def process(input: JValue) = {
    Right(JsonDecode[UpsertResponse].decode(input).right.getOrElse {
      throw new InvalidResponseJsonException(input, "Unable to interpret response as an upsert response")
    })
  }

  def endOfInput() =
    throw new MalformedResponseJsonException("End of input while awaiting upsert response")
}
