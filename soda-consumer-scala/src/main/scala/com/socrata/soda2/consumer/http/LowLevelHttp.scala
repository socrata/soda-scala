package com.socrata.soda2.consumer.http

import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import scala.collection.JavaConverters._
import scala.io.Codec

import java.net.{URI, URL}

import com.ning.http.client.AsyncHttpClient
import com.rojoma.json.ast.{JValue, JObject}

import com.socrata.soda2.consumer.LowLevel
import com.socrata.soda2.consumer.impl.ResultProducer
import com.socrata.soda2.http._
import com.socrata.http.{JsonEntityWriter, Authorization, Headers}
import com.socrata.http.implicits._
import com.socrata.iteratee.CharIteratee
import com.socrata.soda2.{Resource, Soda2Metadata}
import com.socrata.future.ExecutionContextTimer

// should this be moved to soda2.http?  See similar comment on LowLevel.
class LowLevelHttp(val client: AsyncHttpClient, val logicalHost: String, val physicalHost: String, val port: Int, val secure: Boolean, val authorization: Authorization)(implicit executionContext: ExecutionContext, timer: ExecutionContextTimer) extends LowLevel {
  import LowLevelHttp._

  def get[T](resource: Resource, getParameters: Map[String, Seq[String]], iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] =
    get(uriForResource(resource), resource, Some(getParameters), noCallback, iteratee)

  def get[T](uri: URI, originalResource: Resource, queryParameters: Option[Map[String, Seq[String]]], progressCallback: JObject => Unit, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] = {
    log.debug("Making GET request to {}", uri)
    queryParameters.foreach{ p => log.debug("With query parameters {}", com.rojoma.json.util.JsonUtil.renderJson(p)) }
    client.prepareGet(uri.toURL.toString).
      maybeSetQueryParametersS(queryParameters).
      setFollowRedirects(true).
      setHeader("Accept", "application/json").
      setHeader("X-Socrata-Host", logicalHost).
      authorize(authorization).
      makeRequest(new StandardConsumer(originalResource, bodyConsumer(_, _, iteratee(uri, _)))).
      flatMap(maybeRetryGet(uri, originalResource, queryParameters, progressCallback, iteratee, _))
  }

  def postForm[T](uri: URI, originalResource: Resource, formParameters: Option[Map[String, Seq[String]]], progressCallback: JObject => Unit, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] = {
    log.debug("Making POST request to {}", uri)
    formParameters.foreach{ p => log.debug("With form parameters {}", com.rojoma.json.util.JsonUtil.renderJson(p)) }
    client.preparePost(uri.toString).
      maybeSetParametersS(formParameters).
      setFollowRedirects(true).
      setHeader("Accept", "application/json").
      setHeader("X-Socrata-Host", logicalHost).
      authorize(authorization).
      makeRequest(new StandardConsumer(originalResource, bodyConsumer(_, _, iteratee(uri, _)))).
      flatMap(maybeRetryForm(uri, originalResource, formParameters, progressCallback, iteratee, _))
  }

  def postJson[T](resource: Resource, body: JValue, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] =
    postJson(uriForResource(resource), resource, None, body, noCallback, iteratee)

  def postJson[T](uri: URI, originalResource: Resource, queryParameters: Option[Map[String, Seq[String]]], body: JValue, progressCallback: JObject => Unit, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] =
    postPutJson(client.preparePost(uri.toString), true, uri, originalResource, queryParameters, body, progressCallback, iteratee)

  def putJson[T](resource: Resource, body: JValue, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] =
    putJson(uriForResource(resource), resource, None, body, noCallback, iteratee)

  def putJson[T](uri: URI, originalResource: Resource, queryParameters: Option[Map[String, Seq[String]]], body: JValue, progressCallback: JObject => Unit, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] =
    postPutJson(client.preparePut(uri.toString), false, uri, originalResource, queryParameters, body, progressCallback, iteratee)

  def postPutJson[T](builder: AsyncHttpClient#BoundRequestBuilder, isPost: Boolean, uri: URI, originalResource: Resource, queryParameters: Option[Map[String, Seq[String]]], body: JValue, progressCallback: JObject => Unit, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] = {
    log.debug("Making JSON {} request to {}", (if(isPost) "POST" else "PUT"), uri)
    queryParameters.foreach{ p => log.debug("With query parameters {}", com.rojoma.json.util.JsonUtil.renderJson(p)) }
    builder.
      maybeSetQueryParametersS(queryParameters).
      setFollowRedirects(true).
      setHeader("Accept", "application/json").
      setHeader("Content-type", "application/json; charset=utf-8").
      setHeader("X-Socrata-Host", logicalHost).
      authorize(authorization).
      setBody(new JsonEntityWriter(body)).
      makeRequest(new StandardConsumer(originalResource, bodyConsumer(_, _, iteratee(uri, _)))).
      flatMap(maybeRetryJson(isPost, uri, originalResource, queryParameters, body, progressCallback, iteratee, _))
  }

  def bodyConsumer[T](headers: Headers, codec: Codec, iteratee: Soda2Metadata => CharIteratee[T]) = {
    val prefix = "x-soda2-"
    val soda2Metadata = new com.ning.http.client.FluentCaseInsensitiveStringsMap
    for {
      (h, vs) <- headers if h.toLowerCase.startsWith(prefix)
    } soda2Metadata.add(h.substring(prefix.length), vs.asJavaCollection)

    val metadataAsMap: java.util.Map[String, java.util.List[String]] = soda2Metadata

    new ResultProducer(codec, iteratee(metadataAsMap.asScala.mapValues(_.asScala.last)))
  }

  def protocol = if(secure) "https" else "http"

  def uriForPath(path: String) =
    new URL(protocol, physicalHost, port, path).toURI

  def uriForResource(resource: Resource) =
    uriForPath("/id/" + resource)

  def maybeRetry[T](x: Retryable[T], progressCallback: JObject => Unit)(onRetry: NewRequest => Future[T]): Future[T] = x match {
    case Right(result) =>
      Future.successful(result)
    case Left(newRequest) =>
      log.debug("Got 202; retrying in {}s", newRequest.retryAfter)
      progressCallback(newRequest.details)
      timer.in(newRequest.retryAfter.seconds) {
        onRetry(newRequest)
      }.flatMap(identity)
  }

  def maybeRetryGet[T](uri: URI, originalResource: Resource, getParameters: Option[Map[String, Seq[String]]], progressCallback: JObject => Unit, iteratee: (URI, Soda2Metadata) => CharIteratee[T], x: Retryable[T]): Future[T] = maybeRetry(x, progressCallback) {
    case Retry(_, _) =>
      get(uri, originalResource, getParameters, progressCallback, iteratee)
    case RetryWithTicket(ticket, _, _) =>
      // If "uri" involved query parameters, this will kill them all in favor of just the ticket.  This
      // should be fine but if not we might need to extract them.  The only case I can think of where
      // this would matter is if a request Redirected to a location-with-query-parameters gets a RetryWithTicket
      // response, which should never ever happen.
      val newParameters = Some(getParameters.getOrElse(Map.empty[String, Seq[String]]) + ("ticket" -> Seq(ticket)))
      get(uri, originalResource, newParameters, progressCallback, iteratee)
    case Redirect(url, _, _) =>
      val target = uri.resolve(url)
      get(target, originalResource, None, progressCallback, iteratee)
  }

  def extractParametersFrom(uri: URI): java.util.Map[String, java.util.List[String]] =
    client.prepareGet(uri.toString).build().getQueryParams // ick, but it works!

  def maybeRetryForm[T](uri: URI, originalResource: Resource, formParameters: Option[Map[String, Seq[String]]], progressCallback: JObject => Unit, iteratee: (URI, Soda2Metadata) => CharIteratee[T], x: Retryable[T]): Future[T] = maybeRetry(x, progressCallback) {
    case Retry(_, _) =>
      postForm(uri, originalResource, formParameters, progressCallback, iteratee)
    case RetryWithTicket(ticket, _, _) =>
      val newParameters = Some(extractParametersFrom(uri).asScala.mapValues(_.asScala).toMap + ("ticket" -> Seq(ticket)))
      get(uri, originalResource, newParameters, progressCallback, iteratee)
    case Redirect(url, _, _) =>
      val target = uri.resolve(url)
      get(target, originalResource, None, progressCallback, iteratee)
  }

  def maybeRetryJson[T](isPost: Boolean, uri: URI, originalResource: Resource, queryParameters: Option[Map[String, Seq[String]]], body: JValue, progressCallback: JObject => Unit, iteratee: (URI, Soda2Metadata) => CharIteratee[T], x: Retryable[T]): Future[T] = maybeRetry(x, progressCallback) {
    case Retry(_, _) =>
      if(isPost) postJson(uri, originalResource, queryParameters, body, progressCallback, iteratee)
      else putJson(uri, originalResource, queryParameters, body, progressCallback, iteratee)
    case RetryWithTicket(ticket, _, _) =>
      val newParameters = Some(queryParameters.getOrElse(Map.empty[String, Seq[String]]) + ("ticket" -> Seq(ticket)))
      get(uri, originalResource, newParameters, progressCallback, iteratee)
    case Redirect(url, _, _) =>
      val target = uri.resolve(url)
      get(target, originalResource, None, progressCallback, iteratee)
  }

  def legacyMakeWorkingCopy[T](resource: Resource, copyRows: Boolean, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] = {
    val method = if(copyRows) "copy" else "copySchema"
    postForm(uriForPath("/views/" + resource + "/publication?method=" + method), resource, None, noCallback, iteratee)
  }

  def legacyPublish[T](resource: Resource, iteratee: (URI, Soda2Metadata) => CharIteratee[T]): Future[T] = {
    val progressCallback = noCallback _
    awaitNoPendingGeocodesFor(resource, progressCallback).flatMap { _ =>
      postForm(uriForPath("/views/" + resource + "/publication"), resource, None, progressCallback, iteratee)
    }
  }

  // uggggghh.  I hate SODA1.
  def awaitNoPendingGeocodesFor(resource: Resource, progressCallback: JObject => Unit): Future[Unit] = {
    import com.rojoma.json.ast._
    import com.socrata.iteratee.JValueIteratee
    import com.socrata.soda2.{InvalidResponseJsonException, MalformedResponseJsonException}
    val jValue = get(
      uriForPath("/api/geocoding"), resource, Some(Map("method" -> Seq("pending"), "id" -> Seq(resource.toString))),
      progressCallback,
      { (_, _) => new JValueIteratee(e => throw new MalformedResponseJsonException("Non-JValue from pending geocode poll", e)) })
    jValue.flatMap {
      case obj: JObject =>
        obj.get("view") match {
          case Some(JNumber(n)) if n == BigDecimal(0) =>
            log.debug("No pending geocodes; the publish can proceed")
            Future.successful(())
          case Some(JNumber(n)) =>
            log.debug("There are still {} pending geocodes; sleeping for 60s", n)
            progressCallback(obj)
            timer.in(60.seconds) {
              awaitNoPendingGeocodesFor(resource, progressCallback)
            }
          case _ =>
            throw new InvalidResponseJsonException(obj, "Uninterpretable JSON from pending geocode poll")
        }
      case datum =>
        throw new InvalidResponseJsonException(datum, "Uninterpretable JSON from pending geocode poll")
    }
  }
}

object LowLevelHttp {
  private val log = org.slf4j.LoggerFactory.getLogger(classOf[LowLevelHttp])

  def noCallback(details: JObject) {}
}
