package com.socrata.soda2

/** A typeclass for things that can be converted to [[com.socrata.soda2.Resource]]s. */
trait ResourceLike[T] {
  /** Converts the value to a [[com.socrata.soda2.Resource]]. */
  def asResource(value: T): Resource
}

object ResourceLike {
  implicit object StringResourceLike extends ResourceLike[String] {
    def asResource(value: String) = Resource(value)
  }

  implicit object ResourceResourceLike extends ResourceLike[Resource] {
    def asResource(value: Resource) = value
  }
}
