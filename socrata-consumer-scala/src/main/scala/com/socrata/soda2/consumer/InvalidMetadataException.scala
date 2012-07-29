package com.socrata.soda2.consumer

import com.rojoma.json.ast.JString

import com.socrata.soda2.SodaProtocolException

/** Thrown if a response does not contain the correct SODA2 metadata. */
abstract class InvalidMetadataException(msg: String, cause: Throwable = null)
  extends SodaProtocolException(msg, cause)

class MissingMetadataField(val field: String, cause: Throwable = null)
  extends InvalidMetadataException("No metadata field " + JString(field), cause)

class MalformedMetadataField(val field: String, msg: String, cause: Throwable = null)
  extends InvalidMetadataException("Badly-formatted metadata field " + JString(field) + ": " + msg, cause)

class InvalidMetadataValues(val fields: Set[String], msg: String, cause: Throwable = null)
  extends InvalidMetadataException("Invalid metadata values in " + fields.map(JString).mkString(", ") + ": " + msg, cause)
