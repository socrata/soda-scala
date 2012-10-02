package com.socrata.http

import com.ning.http.client.{AsyncHttpClient, RequestBuilderBase}

import com.socrata.http.impl.{EnrichedRequestBuilder, EnrichedBoundRequestBuilder}

object implicits {
  implicit def enrichRequestBuilder[T <: RequestBuilderBase[T]](builder: T) = new EnrichedRequestBuilder(builder)
  implicit def enrichBoundRequestBuilder(builder: AsyncHttpClient#BoundRequestBuilder) = new EnrichedBoundRequestBuilder(builder)
}
