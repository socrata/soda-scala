package com.socrata.soda2.http
package impl

import scala.{collection => sc}
import scala.io.Codec

import com.ning.http.client.RequestBuilderBase

import com.socrata.http.{BodyConsumer, HeadersConsumer}

import HeadersConsumerUtils._

class OKHeadersConsumer[R <: RequestBuilderBase[R], T](bodyConsumer: Codec => BodyConsumer[T]) extends HeadersConsumer[Retryable[T]] {
  def apply(headers: sc.Map[String, Seq[String]]): Left[BodyConsumer[Retryable[T]], Nothing] = {
    val codec = jsonCodec(headers)
    val realBodyConsumer = bodyConsumer(codec)
    Left(new WrappedBodyConsumer(realBodyConsumer))
  }
}
