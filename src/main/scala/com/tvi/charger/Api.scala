package com.tvi.charger

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.Materializer
import com.tvi.charger.models.Tariff
import com.tvi.charger.models.codecs._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.headers.`Content-Type`
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class Api(apiConfig: ApiConfig, tariffService: TariffService)(
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
          logger.info(s"new tariff, user:$userName, tariff=${Json.toJson(tariff).toString()}")
          val tariffWithUser = tariff.copy(user = userName)
          tariffService.save(tariffWithUser) match {
            case result: TariffSaveResult if result.success => complete(OK, List(`Content-Type`(`application/json`)), tariffWithUser)
            case result: TariffSaveResult => complete(Forbidden, result.reason.getOrElse(""))
          }
        }
      }
    }
}
