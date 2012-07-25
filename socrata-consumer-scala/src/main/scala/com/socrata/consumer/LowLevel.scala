package com.socrata.consumer

import com.socrata.consumer.impl.QueryDisambiguator
import com.socrata.future.Future
import com.socrata.iteratee.CharIteratee

/** Very low-level access to a SODA2 server.  This encapsulates all HTTP actions so higher-level code
 * such as a [[com.socrata.consumer.Simple]] object can deal purely with the data returned. */
trait LowLevel {
  /** Executes a GET query against a SODA2 server and feeds the character data returned into the given
   * [[com.socrata.iteratee.Iteratee]]. */
  def execute[T](resource: String, getParameters: Map[String, Seq[String]], iteratee: CharIteratee[T]): Future[T]

  /** Executes a GET query against a SODA2 server and feeds the character data returned into the given
   * [[com.socrata.iteratee.Iteratee]]. */
  def execute[T](resource: String, getParameters: Map[String, String], iteratee: CharIteratee[T])(implicit stupidErasure: QueryDisambiguator): Future[T] =
    execute(resource, getParameters.mapValues(Seq(_)), iteratee)
}
