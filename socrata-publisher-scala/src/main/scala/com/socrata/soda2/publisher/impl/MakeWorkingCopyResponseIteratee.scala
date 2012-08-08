package com.socrata.soda2.publisher
package impl

import com.rojoma.json.ast.{JString, JObject, JValue}

import com.socrata.iteratee.Iteratee
import com.socrata.soda2.{InvalidResponseJsonException, MalformedResponseJsonException, Resource}

class MakeWorkingCopyResponseIteratee(op: String) extends Iteratee[JValue, Resource] {
  def process(input: JValue) = {
    val uid = for {
      obj <- input.cast[JObject]
      uidValue <- obj.get("id")
      uid <- uidValue.cast[JString]
    } yield Resource(uid.string)

    Right(uid.getOrElse {
      throw new InvalidResponseJsonException(input, "Unable to interpret response as a " + op + " response")
    })
  }

  def endOfInput() =
    throw new MalformedResponseJsonException("End of input while awaiting " + op + " response")
}
