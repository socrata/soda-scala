package com.socrata.soda2.consumer

import com.rojoma.json.ast.{JObject, JValue}
import com.socrata.soda2.{ColumnName, ColumnNameLike}
import com.socrata.soda2.values.{SodaType, SodaValue}

trait Row {
  /** Returns a view of the schema of this result set. */
  def columnTypes: Map[ColumnName, SodaType]

  /** Return the value for the given column, or `None` if the value is null or the
   * name does not exist in ``columnTypes.keys`` and `throwOnUnknownColumn` is false.
   *
   * @throws IllegalArgumentException if the name does not exist in this row and `throwOnUnknownColumn` is true. */
  def apply[T : ColumnNameLike](columnName: T, throwOnUnknownColumn: Boolean = true): Option[SodaValue]

  /** Return the value for the given column, or `orElse` if the value is null or the
   * name does not exist in ``columnTypes.keys`` and `throwOnUnknownColumn` is false.
   *
   * @throws IllegalArgumentException if the name does not exist in this row and `throwOnUnknownColumn` is true. */
  def getOrElse[T : ColumnNameLike](columnName: T, orElse: => SodaValue, throwOnUnknownColumn: Boolean = true): SodaValue

  /** Provides a view of the entire row as a raw JSON object */
  def asJObject: JObject

  /** Provides a view of the entire row as a raw JSON object */
  def asMap: Map[ColumnName, Option[SodaValue]]

  override def toString = asJObject.toString
}
