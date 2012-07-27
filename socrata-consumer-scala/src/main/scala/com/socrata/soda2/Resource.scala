package com.socrata.soda2

/** An object representing a SODA2 resource identifier. */
class Resource(name: String) {
  // TODO: check that "name" is a valid resource-name
  override def toString = name

  override def equals(o: Any) = o match {
    case that: Resource => this.toString == that.toString
    case _ => false
  }

  override def hashCode = toString.hashCode
}

object Resource {
  def apply(name: String) = new Resource(name)
}
