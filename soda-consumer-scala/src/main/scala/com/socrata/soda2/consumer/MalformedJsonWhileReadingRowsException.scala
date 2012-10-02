package com.socrata.soda2.consumer

import com.socrata.soda2.MalformedResponseJsonException

/** Thrown if bad JSON is encountered during the reading of row data from a query. */
class MalformedJsonWhileReadingRowsException(cause: Throwable)
  extends MalformedResponseJsonException("Malformed JSON encountered while reading rows", cause)
