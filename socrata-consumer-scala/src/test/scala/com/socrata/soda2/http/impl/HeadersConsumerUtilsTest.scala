package com.socrata.soda2.http.impl

import java.nio.ByteBuffer

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import com.socrata.soda2.http.{IllegalResponseCharsetNameException, UnsupportedResponseCharsetException}

class HeadersConsumerUtilsTest extends WordSpec with MustMatchers {
  "codecFor" should {
    "return a UTF-8 decoder for charset=utf-8" in {
      val bytes = Array(0xe2, 0x82, 0xac).map(_.toByte)
      val decoded = HeadersConsumerUtils.codecFor("utf-8").decoder.decode(ByteBuffer.wrap(bytes))
      decoded.toString must equal ("\u20ac")
    }

    "return a Latin1 decoder for charset=iso-8859-1" in {
      val bytes = Array(0xe2, 0x82, 0xac).map(_.toByte)
      val decoded = HeadersConsumerUtils.codecFor("iso-8859-1").decoder.decode(ByteBuffer.wrap(bytes))
      decoded.toString must equal ("\u00e2\u0082\u00ac")
    }

    "throw an UnsupportedResponseCharsetException for a non-charset" in {
      evaluating(HeadersConsumerUtils.codecFor("not-a-charset")) must produce[UnsupportedResponseCharsetException]
    }

    "throw an IllegalResponseCharsetNameException for a malformed charset name" in {
      evaluating(HeadersConsumerUtils.codecFor("I am not a charset")) must produce[IllegalResponseCharsetNameException]
    }
  }
}
