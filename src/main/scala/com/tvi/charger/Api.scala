package com.tvi.charger

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.directives.{Credentials, LogEntry}
import akka.stream.Materializer
import com.tvi.charger.models.Tariff
import com.tvi.charger.models.codecs._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json

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

  def simpleAuthenticator(credentials: Credentials): Option[String] =
    credentials match {
      case p@Credentials.Provided(id) if p.verify("password") => Some(id)
      case _ => None
    }

  implicit def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case NonFatal(e) =>
        logger.error(message = e.getMessage, cause = e)
        complete(HttpResponse(InternalServerError, entity = "server encountered an error, we are monitoring it."))
    }

  def logEntry(req: HttpRequest): LogEntry = {
    LogEntry(s"${req.method.value} ${req.uri.path}", Logging.InfoLevel)
  }

  private val route =
    path("tariffs") {
      authenticateBasic(realm = "tariff space", simpleAuthenticator) { userName =>
        (post & entity(as[Tariff])) { tariff =>
          logRequest(logEntry _) {
            val tariffWithUser = tariff.copy(user = userName)
            logger.info(s"new tariff, user:$userName, tariff=${Json.toJson(tariffWithUser).toString()}")
            tariffService.save(tariffWithUser) match {
              case result: TariffSaveResult if result.success => complete(OK, tariffWithUser)
              case result: TariffSaveResult => complete(Forbidden, result.reason.getOrElse(""))
            }
          }
        }
      }
    }
}
