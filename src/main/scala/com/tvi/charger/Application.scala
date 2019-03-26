package com.tvi.charger

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.event.Logging
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationDouble

object Application extends App {
  implicit val system: ActorSystem = ActorSystem()
  val logger = Logging(system.eventStream, "dependable-charger")

  val materializerSettings = ActorMaterializerSettings(system)
  implicit val materializer: ActorMaterializer = ActorMaterializer(materializerSettings)

  val config = ConfigFactory.load
  val apiConfig = ApiConfig(config, system)
  val tariffService = new TariffService()

  private val eventualBinding = new Api(apiConfig, tariffService).init()

  CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseServiceUnbind, "service_shutdown") { () =>
    logger.info("shutting down gracefully, terminating connections")
    eventualBinding.flatMap(_.terminate(hardDeadline = 30.second)).map { _ =>
      Done
    }
  }
}
