package com.socrata.soda2.consumer

import scala.collection.mutable.MapBuilder

import com.socrata.soda2.consumer.impl.{FieldValue, QueryRunner}
import com.socrata.future.Future
import com.socrata.iteratee._
import com.socrata.soda2.{ResourceLike, Resource}

/** A very high-level interface for running queries against a SODA2 service. */
class Consumer(val lowLevel: LowLevel) {
  /** Prepares to run the given simple query against the given resource.  A "simple query"
   * is one which does only exact-equality comparisons on untransformed columns, and returns
   * the entire rows which match.
   *
   * @note This does not actually run the query.  Instead it produces an object which can be used to
   *       run the query and feed the results into a data consumer.
   * @return An object which can be used to run the query. */
  def query[T](resource: T, parameter: FieldValue, parameters: FieldValue*)(implicit ev: ResourceLike[T]) =
    new SimpleQuery(lowLevel, ev.asResource(resource), (parameter +: parameters).map { fv => (fv.column.toString, fv.value) })

  /** Prepares to run raw SoQL against the given resource.
   *
   * @note This does not actually run the query.  Instead it produces an object which can be used to
   *       run the query and feed the results into a data consumer.
   * @note The library does no syntactic validation of the SoQL.  It is simply passed verbatim to
   *       the SODA service.
   * @return An object which can be used to run the query. */
  def query[T](resource: T, soql: String)(implicit ev: ResourceLike[T]) = {
    new SoQLQuery(lowLevel, ev.asResource(resource), soql)
  }

  /** Sets up a builder that can be used to construct and run a complex SoQL query.
   *
   * @return An object which can be used to build and run the query. */
  def query[T](resource: T)(implicit ev: ResourceLike[T]) =
    new SimpleQuery(lowLevel, ev.asResource(resource), Nil) // TODO: return a QueryBuilder instead
}

class SimpleQuery(lowLevel: LowLevel, resource: Resource, parameters: Seq[(String, String)]) extends QueryRunner(lowLevel) {
  protected def executeQuery[T](iteratee: CharIteratee[T]): Future[T] =
    lowLevel.execute(
      resource,
      parameters.toMap,
      iteratee)
}

class SoQLQuery(lowLevel: LowLevel, resource: Resource, soql: String) extends QueryRunner(lowLevel) {
  protected def executeQuery[T](iteratee: CharIteratee[T]): Future[T] =
    lowLevel.execute(
      resource,
      Map("$query" -> soql),
      iteratee)
}
