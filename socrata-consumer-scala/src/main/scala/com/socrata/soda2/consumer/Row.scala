package com.socrata.soda2.consumer

import com.rojoma.json.ast.{JObject, JValue}
import com.socrata.soda2.ColumnNameLike

/**
 * @note This is a very rough interface; it will be replaced with something
 *       more specific later.  In particular, types will become more concrete
 *       and values into something more SODA-specific than "JValue".
 */
trait Row {
  /** Returns a view of the schema of this result set. */
  def columnTypes: Map[String, String]

  /** Return the value for the given column.  If the column name does not exist in
   * `columnTypes.keys` then this throws a NoSuchElementException. */
  def apply[T : ColumnNameLike](columnName: T): JValue

  /** Return the value for the given column, or `None` if the name does not exist
   * in ``columnTypes.keys``. */
  def get[T : ColumnNameLike](columnName: T): Option[JValue]

  /** Return the value for the given column, or `orElse` if the name does not exist
   * in ``columnTypes.keys``. */
  def getOrElse[T : ColumnNameLike](columnName: T, orElse: => JValue): JValue

  /** Provides a view of the entire row as a raw JSON object */
  def asJObject: JObject
}
