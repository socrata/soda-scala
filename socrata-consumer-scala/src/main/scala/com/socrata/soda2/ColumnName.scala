package com.socrata.soda2

/** An object representing a SODA2 dataset column name */
class ColumnName(name: String) {
  // TODO: ensure "name" is a valid column name
  override def toString = name

  override def equals(o: Any) = o match {
    case that: ColumnName => this.toString == that.toString
    case _ => false
  }

  override def hashCode = toString.hashCode
}

object ColumnName {
  def apply(name: String) = new ColumnName(name)
}
