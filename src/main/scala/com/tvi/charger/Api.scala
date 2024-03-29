package com.tvi.charger

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.directives.{Credentials, LogEntry}
import akka.stream.Materializer
import com.tvi.charger.models.codecs._
import com.tvi.charger.models.{ChargeSession, Tariff, User}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class Api(apiConfig: ApiConfig, tariffService: TariffService, billingService: BillingService)(
  implicit val actorSystem: ActorSystem,
  val materializer: Materializer,
  val executionContext: ExecutionContext
) extends PlayJsonSupport {
  val logger = Logging(actorSystem.eventStream, "charger-api")

  def init(): Future[Http.ServerBinding] = {
    val (host, port) = (apiConfig.host, apiConfig.port)
    Http().bindAndHandle(route, host, port).map { binding =>
      logger.info(s"charger api is listening on $host:$port")
      binding
    }
  }

  private def simpleAuthenticator(credentials: Credentials): Option[String] =
    credentials match {
      case p@Credentials.Provided(id) if p.verify("password") => Some(id)
      case _ => None
    }

  private implicit def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case NonFatal(e) =>
        logger.error(message = e.getMessage, cause = e)
        complete(HttpResponse(InternalServerError, entity = "server encountered an error, we are monitoring it."))
    }

  private def logEntry(req: HttpRequest) = {
    LogEntry(s"${req.method.value} ${req.uri.path}", Logging.InfoLevel)
  }

  private[charger] val route =
    path("tariffs") {
      authenticateBasic(realm = "tariff space", simpleAuthenticator) { userName =>
        (post & entity(as[Tariff])) { tariff =>
          logRequest(logEntry _) {
            val tariffWithUser = tariff.copy(user = User(userName))
            logger.info(s"new tariff, user:$userName, tariff=${Json.toJson(tariffWithUser).toString()}")
            tariffService.save(tariffWithUser) match {
              case result: TariffSaveResult if result.success => complete(OK, tariffWithUser)
              case result: TariffSaveResult => complete(Forbidden, result.reason.getOrElse(""))
            }
          }
        }
      }
    } ~ path("sessions") {
      (post & entity(as[ChargeSession])) { session =>
        logRequest(logEntry _) {
          headerValueByName("X-User") { user =>
            val sessionWithUser = session.copy(user = User(user))
            logger.info(s"new session, session=${Json.toJson(sessionWithUser).toString()}")
            val chargingBill = billingService.saveAndCharge(sessionWithUser, tariffService.getTariff(session.startDate))
            complete(OK, chargingBill)
          }
        }
      } ~
        get {
          logRequest(logEntry _) {
            headerValueByName("X-User") { user =>
              val headers = List(RawHeader("Content-Disposition", s"attachment; filename=billing-report.csv"))
              val source = billingService.billingReportAsSource(User(user))
              respondWithHeaders(headers) {
                complete(HttpEntity(ContentTypes.`text/csv(UTF-8)`, source))
              }
            }
          }
        }
    }
}
