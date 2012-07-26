package com.socrata.soda2.consumer

import com.rojoma.json.ast.{JObject, JValue}

trait Row {
  def columnTypes: Map[String, String]
  def apply(columnName: String): JValue
  def asJObject: JObject
}
