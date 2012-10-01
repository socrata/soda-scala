package com.socrata.soda2

object Util {
  /** Quotes a string to ensure it is a valid SoQL literal */
  def quote(s: String) = "'" + s.replaceAll("'", "''") + "'"
}
