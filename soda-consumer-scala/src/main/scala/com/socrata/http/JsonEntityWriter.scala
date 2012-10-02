package com.socrata.http

import java.io.{OutputStreamWriter, OutputStream}

import com.ning.http.client.Request
import com.rojoma.json.ast.JValue
import com.rojoma.json.util.JsonUtil

class JsonEntityWriter(body: JValue, pretty: Boolean = false) extends Request.EntityWriter {
  def writeEntity(out: OutputStream) {
    val w = new OutputStreamWriter(out, "utf-8")
    try {
      JsonUtil.writeJson(w, body, pretty = pretty, buffer = true)
    } finally {
      w.flush()
    }
  }
}
