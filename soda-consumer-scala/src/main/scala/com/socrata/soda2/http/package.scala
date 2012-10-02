package com.socrata.soda2

package object http {
  /** A type alias for the result of [[com.socrata.soda2.http.StandardConsumer]], representing either
   * the final value produced or the need to make another HTTP request. */
  type Retryable[+T] = Either[NewRequest, T]
}
