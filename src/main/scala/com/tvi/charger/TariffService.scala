package com.tvi.charger

import java.time.Instant

import akka.actor.ActorSystem
import akka.event.Logging
import com.tvi.charger.models.Tariff

class TariffService(tariffRepository: TariffRepository)(implicit val actorSystem: ActorSystem) {
  private val logger = Logging(actorSystem.eventStream, "tariff-service")

  def save(tariff: Tariff): TariffSaveResult = {
    val tariffSaveResult = tariffRepository.save(tariff)
    if (tariffSaveResult.success) {
      logger.info(s"tariff saved, tariff=$tariff")
    } else {
      logger.info(s"tariff not saved, reason=${tariffSaveResult.reason}, tariff=$tariff")
    }
    tariffSaveResult
  }

  def getTariff(date: Instant): Tariff = {
    tariffRepository.getClosestTariff(date)
  }

}
