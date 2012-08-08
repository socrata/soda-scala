package com.socrata.soda2.consumer.http

import scala.collection.JavaConverters._
import scala.io.Codec

import java.net.{URI, URL}

import com.ning.http.client.AsyncHttpClient
import com.rojoma.json.ast.JValue

import com.socrata.soda2.consumer.LowLevel
import com.socrata.soda2.consumer.impl.ResultProducer
import com.socrata.soda2.http._
import com.socrata.future.{ExecutionContext, Future}
import com.socrata.http.{JsonEntityWriter, Authorization, Headers}
import com.socrata.http.implicits._
import com.socrata.iteratee.CharIteratee
import com.socrata.soda2.{Resource, Soda2Metadata}

// should this be moved to soda2.http?  See similar comment on LowLevel.
class LowLevelHttp(val client: AsyncHttpClient, val host: String, val port: Int, val authorization: Authorization)(implicit executionContext: ExecutionContext) extends LowLevel {
  import LowLevelHttp._

  def get[T](resource: Resource, getParameters: Map[String, Seq[String]], iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] =
    get(uriForResource(resource), resource, Some(getParameters), iteratee)

  def get[T](uri: URI, originalResource: Resource, queryParameters: Option[Map[String, Seq[String]]], iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] = {
    val builder = client.prepareGet(uri.toURL.toString).
      maybeSetQueryParametersS(queryParameters).
      setFollowRedirects(true).
      setHeader("Accept", "application/json").
      authorize(authorization)
    log.debug("Making request to {}", uri)
    queryParameters.foreach{ p => log.debug("With query parameters {}", com.rojoma.json.util.JsonUtil.renderJson(p)) }
    builder.
      makeRequest(new StandardConsumer(originalResource, bodyConsumer(_, _, iteratee(uri, _)))).
      flatMap(maybeRetryGet(uri, originalResource, queryParameters, iteratee, _))
  }

  def postForm[T](uri: URI, originalResource: Resource, formParameters: Option[Map[String, Seq[String]]], iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] = {
    val builder = client.preparePost(uri.toString).
      maybeSetParametersS(formParameters).
      setFollowRedirects(true).
      setHeader("Accept", "application/json").
      authorize(authorization)
    log.debug("Making request to {}", uri)
    formParameters.foreach{ p => log.debug("With form parameters {}", com.rojoma.json.util.JsonUtil.renderJson(p)) }
    builder.
      makeRequest(new StandardConsumer(originalResource, bodyConsumer(_, _, iteratee(uri, _)))).
      flatMap(maybeRetryForm(uri, originalResource, formParameters, iteratee, _))
  }

  def postJson[T](resource: Resource, body: JValue, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] =
    postJson(uriForResource(resource), resource, None, body, iteratee)

  def postJson[T](uri: URI, originalResource: Resource, queryParameters: Option[Map[String, Seq[String]]], body: JValue, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] = {
    val builder = client.preparePost(uri.toString).
      maybeSetQueryParametersS(queryParameters).
      setFollowRedirects(true).
      setHeader("Accept", "application/json").
      setHeader("Content-type", "application/json; charset=utf-8").
      authorize(authorization).
      setBody(new JsonEntityWriter(body))
    log.debug("Making request to {}", uri)
    builder.
      makeRequest(new StandardConsumer(originalResource, bodyConsumer(_, _, iteratee(uri, _)))).
      flatMap(maybeRetryJson(uri, originalResource, queryParameters, body, iteratee, _))
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

  def maybeRetry[T](x: Retryable[T])(onRetry: NewRequest => Future[T]): Future[T] = x match {
    case Right(result) =>
      Future.now(result)
    case Left(newRequest) =>
      // TODO: some sort of progress based on newRequest.details
      log.debug("Got 202; retrying in {}s", newRequest.retryAfter)
      executionContext.in(newRequest.retryAfter) {
        onRetry(newRequest)
      }.flatten
  }

  def maybeRetryGet[T](uri: URI, originalResource: Resource, getParameters: Option[Map[String, Seq[String]]], iteratee: (URI, Soda2Metadata) => CharIteratee[T], x: Retryable[T]): Future[T] = maybeRetry(x) {
    case Retry(_, _) =>
      get(uri, originalResource, getParameters, iteratee)
    case RetryWithTicket(ticket, _, _) =>
      // If "uri" involved query parameters, this will kill them all in favor of just the ticket.  This
      // should be fine but if not we might need to extract them.  The only case I can think of where
      // this would matter is if a request Redirected to a location-with-query-parameters gets a RetryWithTicket
      // response, which should never ever happen.
      val newParameters = Some(getParameters.getOrElse(Map.empty[String, Seq[String]]) + ("ticket" -> Seq(ticket)))
      get(uri, originalResource, newParameters, iteratee)
    case Redirect(url, _, _) =>
      val target = uri.resolve(url)
      get(target, originalResource, None, iteratee)
  }

  def maybeRetryForm[T](uri: URI, originalResource: Resource, formParameters: Option[Map[String, Seq[String]]], iteratee: (URI, Soda2Metadata) => CharIteratee[T], x: Retryable[T]): Future[T] = maybeRetry(x) {
    case Retry(_, _) =>
      postForm(uri, originalResource, formParameters, iteratee)
    case RetryWithTicket(ticket, _, _) =>
      // If "uri" had baked-in query parameters, this will kill them all in favor of just the ticket.  This
      // should be fine but if not we might need to extract them.  I can't think of a case where that
      // could happen -- even in the case Redirect-followed-by-RetryWithTicket described above in maybeRetryGet,
      // we'd have done a GET here first, so we wouldn't be executing in this place for the second 202.
      val newParameters = Some(Map("ticket" -> Seq(ticket)))
      get(uri, originalResource, newParameters, iteratee)
    case Redirect(url, _, _) =>
      val target = uri.resolve(url)
      get(target, originalResource, None, iteratee)
  }

  def maybeRetryJson[T](uri: URI, originalResource: Resource, queryParameters: Option[Map[String, Seq[String]]], body: JValue, iteratee: (URI, Soda2Metadata) => CharIteratee[T], x: Retryable[T]): Future[T] = maybeRetry(x) {
    case Retry(_, _) =>
      postJson(uri, originalResource, queryParameters, body, iteratee)
    case RetryWithTicket(ticket, _, _) =>
      // If "uri" had baked-in query parameters, this will kill them all in favor of just the ticket.  This
      // should be fine but if not we might need to extract them.  I can't think of a case where that
      // could happen -- even in the case Redirect-followed-by-RetryWithTicket described above in maybeRetryGet,
      // we'd have done a GET here first, so we wouldn't be executing in this place for the second 202.
      val newParameters = Some(queryParameters.getOrElse(Map.empty[String, Seq[String]]) + ("ticket" -> Seq(ticket)))
      get(uri, originalResource, newParameters, iteratee)
    case Redirect(url, _, _) =>
      val target = uri.resolve(url)
      get(target, originalResource, None, iteratee)
  }
}

object LowLevelHttp {
  private val log = org.slf4j.LoggerFactory.getLogger(classOf[LowLevelHttp])
}
