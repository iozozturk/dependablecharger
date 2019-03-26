package com.tvi.charger

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.event.Logging
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationDouble

object Application extends App {
  implicit val actorSystem: ActorSystem = ActorSystem()
  private val logger = Logging(actorSystem.eventStream, "dependable-charger")

  private val materializerSettings = ActorMaterializerSettings(actorSystem)
  implicit val materializer: ActorMaterializer = ActorMaterializer(materializerSettings)

  private val config = ConfigFactory.load
  private val apiConfig = ApiConfig(config, actorSystem)
  private val tariffService = new TariffService()

  private val eventualBinding = new Api(apiConfig, tariffService).init()

  CoordinatedShutdown(actorSystem).addTask(CoordinatedShutdown.PhaseServiceUnbind, "service_shutdown") { () =>
    logger.info("shutting down gracefully, terminating connections")
    eventualBinding.flatMap(_.terminate(hardDeadline = 30.second)).map { _ =>
      Done
    }
  }
}
