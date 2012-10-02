package com.socrata.iteratee

import com.rojoma.json.io.JsonReaderException
import com.rojoma.json.ast.JValue

/** Returns the first complete JSON value that can be read from a character
 *stream. */
class JValueIteratee(onDecodeError: (JsonReaderException => JValue) = JValueIteratee.defaultDecodeErrorAction) extends CharJValueEnumeratee(new IdentityIteratee, onDecodeError)

object JValueIteratee {
  private def defaultDecodeErrorAction(e: JsonReaderException) = throw e
}

