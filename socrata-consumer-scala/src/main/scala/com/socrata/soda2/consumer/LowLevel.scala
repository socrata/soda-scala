package com.socrata.soda2.consumer

import java.net.URI

import com.rojoma.json.ast.JValue

import com.socrata.soda2.consumer.impl.QueryDisambiguator
import com.socrata.future.Future
import com.socrata.iteratee.CharIteratee
import com.socrata.soda2.{Resource, Soda2Metadata}

// Should this be moved to the soda2 package?  It's not a consumer-specific thing...

/** Very low-level access to a SODA2 server.  This encapsulates all HTTP actions so higher-level code
 * such as a [[com.socrata.soda2.consumer.Consumer]] object can deal purely with the data returned. */
trait LowLevel {
  /** Executes a GET query against a SODA2 server and feeds the character data returned into the given
   * [[com.socrata.iteratee.Iteratee]].
   *
   * @param resource The resource to retrieve.
   * @param parameters The values to feed into query.
   * @param iteratee A handler for the response. */
  def get[T](resource: Resource, parameters: Map[String, Seq[String]], iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T]

  /** Executes a GET query against a SODA2 server and feeds the character data returned into the given
   * [[com.socrata.iteratee.Iteratee]].
   *
   * @param resource The resource to retrieve.
   * @param parameters The values to feed into query.
   * @param iteratee A handler for the response. */
  def get[T](resource: Resource, parameters: Map[String, String], iteratee: (URI, Soda2Metadata) => CharIteratee[T])(implicit stupidErasure: QueryDisambiguator): Future[T] =
    get(resource, parameters.mapValues(Seq(_)), iteratee)

  /** Executes a POST query against a SODA2 server and feeds the character data returned into the given
   * [[com.socrata.iteratee.Iteratee]].
   *
   * @param resource The resource to which to send the JSON.
   * @param jvalue The JSON to serialize into the request.
   * @param iteratee A handler for the response. */
  def postJson[T](resource: Resource, jvalue: JValue, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T]
}
