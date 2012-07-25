package com.socrata.http

import scala.{collection => sc}
import scala.io.Codec

import com.ning.http.client.RequestBuilderBase

import HeadersConsumerUtils._

class OKHeadersConsumer[R <: RequestBuilderBase[R], T](bodyConsumer: Codec => BodyConsumer[T]) extends HeadersConsumer[Retryable[T]] {
  def apply(headers: sc.Map[String, Seq[String]]): Left[BodyConsumer[Retryable[T]], Nothing] = {
    jsonCodec(headers) match {
      case Some(codec) =>
        val realBodyConsumer = bodyConsumer(codec)
        Left(new WrappedBodyConsumer(realBodyConsumer))
      case None =>
        error("NYI")
    }
  }
}
