package com.socrata.http

import scala.collection.JavaConverters._

import com.ning.http.client.{HttpResponseBodyPart, HttpResponseHeaders, HttpResponseStatus, AsyncHandler}

/** An [[com.ning.http.client.AsyncHandler]] which uses functions to represent the
 * state-transitions in processing an HTTP result.
 *
 * @param consumer The initial state of the machine.
 */
class NiceAsyncHandler[T](consumer: StatusConsumer[T]) extends AsyncHandler[T] {
  private var statusConsumer: StatusConsumer[T] = consumer
  private var headersConsumer: HeadersConsumer[T] = null
  private var bodyConsumer: BodyConsumer[T] = null

  private var result: Either[Throwable, T] = null

  def onThrowable(t: Throwable) {
    result = Left(t)
  }

  def onStatusReceived(status: HttpResponseStatus): AsyncHandler.STATE = {
    if(statusConsumer == null) throw new IllegalStateException("not expecting status")

    // I don't know if we'll ever get one of these, or if we do if
    // async-http-client will let us know about it -- if we do it'll
    // be a 100 Continue or some sort of informational thing we don't
    // care about.
    if(100 <= status.getStatusCode && status.getStatusCode <= 199) return AsyncHandler.STATE.CONTINUE

    val statusResult = statusConsumer(Status(status.getStatusCode, status.getStatusText, status.getProtocolName, status.getProtocolMajorVersion, status.getProtocolMinorVersion))

    statusConsumer = null

    statusResult match {
      case Left(newHeadersConsumer) =>
        headersConsumer = newHeadersConsumer
        AsyncHandler.STATE.CONTINUE
      case Right(r) =>
        result = Right(r)
        AsyncHandler.STATE.ABORT
    }
  }

  def onHeadersReceived(headers: HttpResponseHeaders): AsyncHandler.STATE = {
    if(headers.isTraillingHeadersReceived) return AsyncHandler.STATE.CONTINUE
    if(headersConsumer == null) throw new IllegalStateException("not expecting headers")
    val headersResult = headersConsumer((headers.getHeaders : java.util.Map[String, java.util.List[String]]).asScala.mapValues(_.asScala))

    headersConsumer = null

    headersResult match {
      case Left(newBodyConsumer) =>
        bodyConsumer = newBodyConsumer
        AsyncHandler.STATE.CONTINUE
      case Right(r) =>
        result = Right(r)
        AsyncHandler.STATE.ABORT
    }
  }

  def onBodyPartReceived(bodyPart: HttpResponseBodyPart): AsyncHandler.STATE = {
    if(bodyConsumer == null) throw new IllegalStateException("not expecting body part")
    bodyConsumer(bodyPart.getBodyPartBytes, bodyPart.isLast) match {
      case Left(newBodyConsumer) =>
        bodyConsumer = newBodyConsumer
        AsyncHandler.STATE.CONTINUE
      case Right(r) =>
        bodyConsumer = null
        result = Right(r)
        AsyncHandler.STATE.ABORT
    }
  }

  def onCompleted(): T = {
    if(bodyConsumer != null) result = Right(bodyConsumer(new Array[Byte](0), isLast = true).right.get)
    result match {
      case Left(err) => throw err
      case Right(res) => res
    }
  }
}
