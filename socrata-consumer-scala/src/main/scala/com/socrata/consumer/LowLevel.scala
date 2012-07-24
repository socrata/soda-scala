package com.socrata
package consumer

import `consumer-impl`._

import iteratee.CharIteratee
import future.Future

trait LowLevel {
  def execute[T](resource: Resource, getParameters: Map[String, Seq[String]], iteratee: CharIteratee[T]): Future[T]

  def execute[T](resource: Resource, getParameters: Map[String, String], iteratee: CharIteratee[T])(implicit stupidErasure: QueryDisambiguator): Future[T] =
    execute(resource, getParameters.mapValues(Seq(_)), iteratee)
}
