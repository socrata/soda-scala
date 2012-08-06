package com.socrata.soda2.consumer.http

import scala.collection.JavaConverters._
import scala.io.Codec

import java.net.{URI, URL}

import com.ning.http.client.AsyncHttpClient
import com.rojoma.json.ast.JValue
import com.rojoma.json.io.CompactJsonWriter

import com.socrata.soda2.consumer.LowLevel
import com.socrata.soda2.consumer.impl.ResultProducer
import com.socrata.soda2.http._
import com.socrata.future.{ExecutionContext, Future}
import com.socrata.http.{Authorization, Headers}
import com.socrata.http.implicits._
import com.socrata.iteratee.CharIteratee
import com.socrata.soda2.{Resource, Soda2Metadata}

// should this be moved to soda2.http?  See similar comment on LowLevel.
class LowLevelHttp(val client: AsyncHttpClient, val host: String, val port: Int, val authorization: Authorization)(implicit executionContext: ExecutionContext) extends LowLevel {
  import LowLevelHttp._

  def execute[T](resource: Resource, getParameters: Map[String, Seq[String]], iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] =
    execute(uriForResource(resource), resource, getParameters, iteratee)

  def execute[T](uri: URI, originalResource: Resource, getParameters: Map[String, Seq[String]], iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] =
    doGet(uri, originalResource, Some(getParameters), iteratee).flatMap(maybeRetry(uri, originalResource, getParameters, iteratee, _))

  def executeJson[T](resource: Resource, body: JValue, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] =
    executeJson(uriForResource(resource), resource, body, iteratee)

  def executeJson[T](uri: URI, originalResource: Resource, body: JValue, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] =
    doPost(uri, originalResource, body, iteratee).flatMap(maybeRetryJson(uri, originalResource, body, iteratee, _))

  def doGet[T](uri: URI, originalResource: Resource, queryParameters: Option[Map[String, Seq[String]]], iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[Retryable[T]] = {
    val builder = client.prepareGet(uri.toURL.toString).
      maybeSetQueryParametersS(queryParameters).
      setFollowRedirects(true).
      setHeader("Accept", "application/json").
      authorize(authorization)
    log.debug("Making request to {}", uri)
    queryParameters.foreach{ p => log.debug("With query parameters {}", com.rojoma.json.util.JsonUtil.renderJson(p)) }
    builder.makeRequest(new StandardConsumer(originalResource, bodyConsumer(_, _, iteratee(uri, _))))
  }

  def doPost[T](uri: URI, originalResource: Resource, body: JValue, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[Retryable[T]] = {
    val builder = client.preparePost(uri.toString).
      setFollowRedirects(true).
      setHeader("Accept", "application/json").
      setHeader("Content-type", "application/json; charset=utf-8").
      authorize(authorization).
      setBody(CompactJsonWriter.toString(body).getBytes("utf-8"))
    log.debug("Making request to {}", uri)
    builder.makeRequest(new StandardConsumer(originalResource, bodyConsumer(_, _, iteratee(uri, _))))
  }

  def bodyConsumer[T](headers: Headers, codec: Codec, iteratee: Soda2Metadata => CharIteratee[T]) = {
    val prefix = "x-soda2-"
    val soda2Metadata: java.util.Map[String, java.util.List[String]] = new com.ning.http.client.FluentCaseInsensitiveStringsMap
    for {
      (h, vs) <- headers
      if(h.toLowerCase.startsWith(prefix))
    } soda2Metadata.put(h.substring(prefix.length), java.util.Collections.singletonList(vs.last))

    new ResultProducer(codec, iteratee(soda2Metadata.asScala.mapValues(_.get(0))))
  }

  val protocol =
    if(java.lang.Boolean.getBoolean("com.socrata.soda2.forceHttp")) "http"
    else "https"

  def uriForResource(resource: Resource) =
    new URL(protocol, host, port, "/id/" + resource.toString).toURI

  def maybeRetry[T](uri: URI, originalResource: Resource, getParameters: Map[String, Seq[String]], iteratee: (URI, Soda2Metadata) => CharIteratee[T], x: Retryable[T]): Future[T] = x match {
    case Right(result) =>
      Future.now(result)
    case Left(newRequest) =>
      // TODO: some sort of progress based on newRequest.details
      log.debug("Got 202; retrying in {}s", newRequest.retryAfter)
      executionContext.in(newRequest.retryAfter) {
        newRequest match {
          case Retry(_, _) =>
            execute(uri, originalResource, getParameters, iteratee)
          case RetryWithTicket(ticket, _, _) =>
            execute(uri, originalResource, getParameters + ("ticket" -> Seq(ticket)), iteratee)
          case Redirect(url, _, _) =>
            val target = uri.resolve(url)
            doGet(target, originalResource, None, iteratee).flatMap(maybeRetry(target, originalResource, getParameters, iteratee, _))
        }
      }.flatten
  }

  def maybeRetryJson[T](uri: URI, originalResource: Resource, body: JValue, iteratee: (URI, Soda2Metadata) => CharIteratee[T], x: Retryable[T]): Future[T] = x match {
    case Right(result) =>
      Future.now(result)
    case Left(newRequest) =>
      // TODO: some sort of progress based on newRequest.details
      log.debug("Got 202; retrying in {}s", newRequest.retryAfter)
      executionContext.in(newRequest.retryAfter) {
        newRequest match {
          case Retry(_, _) =>
            executeJson(uri, originalResource, body, iteratee)
          case RetryWithTicket(ticket, _, _) =>
            execute(uri, originalResource, Map("ticket" -> Seq(ticket)), iteratee)
          case Redirect(url, _, _) =>
            val target = uri.resolve(url)
            doGet(target, originalResource, None, iteratee).flatMap(maybeRetry(target, originalResource, Map.empty, iteratee, _))
        }
      }.flatten
  }
}

object LowLevelHttp {
  private val log = org.slf4j.LoggerFactory.getLogger(classOf[LowLevelHttp])
}
