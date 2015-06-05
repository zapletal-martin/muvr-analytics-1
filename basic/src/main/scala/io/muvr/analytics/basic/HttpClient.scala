package io.muvr.analytics.basic

import akka.actor.ActorSystem
import spray.http.StatusCodes._
import spray.http.{HttpRequest, HttpResponse, Uri}

import scala.concurrent.Future

/**
 * Spark HttpClient
 */
trait HttpClient {
  import spray.client.pipelining._

  private implicit lazy val actorSystem = ActorSystem("spray-client")

  private implicit lazy val ec = actorSystem.dispatcher

  private lazy val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  /**
   * Creates a Http request to given uri
   * @param requestBuilder builds request, i.e. GET/POST/..,path etc.
   * @return Future of Right if status was 200 or Left otherwise
   */
  def request(requestBuilder: Uri ⇒ HttpRequest): Unit = {
    val uri = Uri(s"http://localhost:12551")

    val request = requestBuilder(uri)

    pipeline(requestBuilder(uri)).map(_.status match {
      case OK ⇒ println(s"Request succeeded")
      case s ⇒  println(s"Request failed with status $s")
    })
  }
}