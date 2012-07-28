package com.socrata.soda2.http
package impl

import scala.io.Codec

import com.ning.http.client.RequestBuilderBase

import com.socrata.http.{BodyConsumer, HeadersConsumer, Headers}

import HeadersConsumerUtils._

class OKHeadersConsumer[R <: RequestBuilderBase[R], T](bodyConsumer: Codec => BodyConsumer[T]) extends HeadersConsumer[Retryable[T]] {
  def apply(headers: Headers): Left[BodyConsumer[Retryable[T]], Nothing] = {
    val codec = jsonCodec(headers)
    val realBodyConsumer = bodyConsumer(codec)
    Left(new WrappedBodyConsumer(realBodyConsumer))
  }
}
