package com.socrata.soda2.consumer
package impl

/** Passed as an implicit parameter to certain overloaded methods which would otherwise
 * collide due to erasure. */
class QueryDisambiguator

object QueryDisambiguator extends QueryDisambiguator {
  implicit val i = this
}
