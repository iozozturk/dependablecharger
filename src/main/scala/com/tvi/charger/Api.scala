package com.tvi.charger

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.Materializer
import com.tvi.charger.models.Tariff
import com.tvi.charger.models.codecs._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class Api(apiConfig: ApiConfig)(
  implicit val system: ActorSystem,
  val materializer: Materializer,
  val executionContext: ExecutionContext
) extends PlayJsonSupport {
  val logger = Logging(system.eventStream, "charger-api")

  def init(): Future[Http.ServerBinding] = {
    val (host, port) = (apiConfig.host, apiConfig.port)
    Http().bindAndHandle(route, host, port).map { binding =>
      logger.info(s"charger api is listening on $host:$port")
      binding
    }
  }

  def myUserPassAuthenticator(credentials: Credentials): Option[String] =
    credentials match {
      case p@Credentials.Provided(id) if p.verify("password") => Some(id)
      case _ => None
    }

  implicit def myExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case NonFatal(e) =>
        logger.error(message = e.getMessage, cause = e)
        complete(HttpResponse(InternalServerError, entity = "server encountered an error, we are monitoring it."))
    }

  private val route =
    path("tariffs") {
      authenticateBasic(realm = "tariff space", myUserPassAuthenticator) { userName =>
        (post & entity(as[Tariff])) { tariff =>
          logger.info(s"new tariff for user:$userName, tariff=$tariff")
          complete(OK)
        }
      }
    }
}
