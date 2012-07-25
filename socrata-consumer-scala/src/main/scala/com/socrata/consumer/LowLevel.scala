package com.socrata.consumer

import com.socrata.consumer.impl.QueryDisambiguator
import com.socrata.future.Future
import com.socrata.iteratee.CharIteratee

trait LowLevel {
  def execute[T](resource: String, getParameters: Map[String, Seq[String]], iteratee: CharIteratee[T]): Future[T]

  def execute[T](resource: String, getParameters: Map[String, String], iteratee: CharIteratee[T])(implicit stupidErasure: QueryDisambiguator): Future[T] =
    execute(resource, getParameters.mapValues(Seq(_)), iteratee)
}
