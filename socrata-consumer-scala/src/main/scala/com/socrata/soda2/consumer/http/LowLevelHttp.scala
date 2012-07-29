package com.socrata.soda2.consumer.http

import scala.collection.JavaConverters._
import scala.io.Codec

import java.net.URL

import com.ning.http.client.AsyncHttpClient

import com.socrata.soda2.consumer.LowLevel
import com.socrata.soda2.consumer.impl.ResultProducer
import com.socrata.soda2.http._
import com.socrata.future.{ExecutionContext, Future}
import com.socrata.http.{Authorization, Headers}
import com.socrata.http.implicits._
import com.socrata.iteratee.CharIteratee
import com.socrata.soda2.{Resource, Soda2Metadata}

// should this be moved to soda2.http?
class LowLevelHttp(val client: AsyncHttpClient, val host: String, val port: Int, val authorization: Authorization)(implicit executionContext: ExecutionContext) extends LowLevel {
  import LowLevelHttp._

  def execute[T](resource: Resource, getParameters: Map[String, Seq[String]], iteratee: Soda2Metadata => CharIteratee[T]): Future[T] =
    doGet(urlForResource(resource).toString, resource, Some(getParameters), iteratee).flatMap(maybeRetry(resource, getParameters, iteratee, _))

  def doGet[T](url: String, resource: Resource, queryParameters: Option[Map[String, Seq[String]]], iteratee: Soda2Metadata => CharIteratee[T]): Future[Retryable[T]] = {
    val builder = client.prepareGet(url).
      maybeSetQueryParametersS(queryParameters).
      setFollowRedirects(true).
      setHeader("Accept", "application/json").
      authorize(authorization)
    log.debug("Making request to {}", url)
    queryParameters.foreach{ p => log.debug("With query parameters {}", com.rojoma.json.util.JsonUtil.renderJson(p)) }
    builder.makeRequest(new StandardConsumer(resource, bodyConsumer(_, _, iteratee)))
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

  def urlForResource(resource: Resource) =
    new URL("https", host, port, resource.toString)

  def maybeRetry[T](resource: Resource, getParameters: Map[String, Seq[String]], iteratee: Soda2Metadata => CharIteratee[T], x: Retryable[T]): Future[T] = x match {
    case Right(result) =>
      Future.now(result)
    case Left(newRequest) =>
      // TODO: some sort of progress based on newRequest.details
      log.debug("Got 202; retrying in {}s", newRequest.retryAfter)
      executionContext.in(newRequest.retryAfter) {
        newRequest match {
          case Retry(_, _) =>
            execute(resource, getParameters, iteratee)
          case RetryWithTicket(ticket, _, _) =>
            execute(resource, getParameters + ("ticket" -> Seq(ticket)), iteratee)
          case Redirect(url, _, _) =>
            val target = urlForResource(resource).toURI.resolve(url).toURL.toString
            doGet(target, resource, None, iteratee).flatMap(maybeRetry(resource, getParameters, iteratee, _))
        }
      }.flatten
  }
}

object LowLevelHttp {
  private val log = org.slf4j.LoggerFactory.getLogger(classOf[LowLevelHttp])
}
