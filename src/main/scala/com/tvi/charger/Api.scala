package com.tvi.charger

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.stream.Materializer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.{ExecutionContext, Future}

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

  private val route = get {
    path("/") {
      parameters('userId.?, 'sessionId.?) { (userId, sessionId) =>
        logger.info(s"new report for userId=$userId sessionId=$sessionId")
        complete(OK)
      }
    }
  }
}
