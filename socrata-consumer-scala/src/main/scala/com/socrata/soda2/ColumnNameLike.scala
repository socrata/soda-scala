package com.socrata.soda2

/** A typeclass for things that can be converted to [[com.socrata.soda2.ColumnName]]s. */
trait ColumnNameLike[T] {
  /** Converts the value to a [[com.socrata.soda2.ColumnName]]. */
  def asColumnName(value: T): ColumnName
}

object ColumnNameLike {
  implicit object StringColumnNameLike extends ColumnNameLike[String] {
    def asColumnName(value: String) = ColumnName(value)
  }

  implicit object ColumnNameColumnNameLike extends ColumnNameLike[ColumnName] {
    def asColumnName(value: ColumnName) = value
  }
}
