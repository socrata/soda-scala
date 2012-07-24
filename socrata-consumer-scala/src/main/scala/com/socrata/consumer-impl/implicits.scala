package com.socrata.`consumer-impl`

import com.ning.http.client.{AsyncHttpClient, RequestBuilderBase}

object implicits {
  implicit def enrichRequestBuilder[T <: RequestBuilderBase[T]](builder: T) = new EnrichedRequestBuilder(builder)
  implicit def enrichBoundRequestBuilder(builder: AsyncHttpClient#BoundRequestBuilder) = new EnrichedBoundRequestBuilder(builder)
}
