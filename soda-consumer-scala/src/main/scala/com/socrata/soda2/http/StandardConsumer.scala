package com.socrata.soda2.http

import scala.io.Codec

import com.socrata.future.ExecutionContext
import com.socrata.http.{BodyConsumer, HeadersConsumer, StatusConsumer, Status, Headers}

import impl.{OKHeadersConsumer, AcceptedHeadersConsumer, ErrorHeadersConsumer}
import com.socrata.soda2.Resource

/** A function that produces a state machine that understands the HTTP envelope of a SODA2
 * request.  This handles converting errors into exceptions and manages the details of
 * retrying long-running requests.
 *
 * @param resource The original resource being requested.  Note: this exists purely to support enriching the
 *                 message provided by SODA1 exceptions.  It will go away once SODA1 has been phased out.
 * @param bodyConsumer The bodyConsumer to be used in the event that the request succeeds at the HTTP level.
 * @param defaultRetryAfter The timeout in seconds to use if a 202 response is received with no suggested timeout value.
 * @param execContext A strategy for launching worker asynchronous worker threads.
 */
class StandardConsumer[T](resource: Resource, bodyConsumer: (Headers, Codec) => BodyConsumer[T], defaultRetryAfter: Int = 60)(implicit execContext: ExecutionContext) extends StatusConsumer[Retryable[T]] {
  def apply(status: Status): Either[HeadersConsumer[Retryable[T]], Retryable[T]] = {
    if(status.isSuccess) success(status)
    else if(status.isRedirect) redirect(status)
    else if(status.isClientError) clientError(status)
    else if(status.isServerError) serverError(status)
    else // throw something
      throw new InvalidHttpStatusException(status)
  }

  private def success(status: Status): Either[HeadersConsumer[Retryable[T]], Retryable[T]] = {
    status.code match {
      case 200 | 201 | 203 | 204 | 205 => Left(new OKHeadersConsumer(bodyConsumer))
      case 202 => Left(new AcceptedHeadersConsumer(defaultRetryAfter))
      case other => throw new InvalidHttpStatusException(status);
    }
  }

  private def redirect(status: Status) = {
    // redirects should be handled by the HTTP client library, but if it hasn't been configured
    // or has declined to handle some 3xx, just throw an exception.
    throw new InvalidHttpStatusException(status)
  }

  private def clientError(status: Status) = {
    Left(new ErrorHeadersConsumer(resource, status.code))
  }

  private def serverError(status: Status) = {
    Left(new ErrorHeadersConsumer(resource, status.code))
  }
}
