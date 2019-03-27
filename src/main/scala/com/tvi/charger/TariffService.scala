package com.tvi.charger

import java.time.Instant

import akka.actor.ActorSystem
import akka.event.Logging
import com.tvi.charger.models.Tariff

import scala.collection.mutable

case class TariffSaveResult(success: Boolean, reason: Option[String])

class TariffService()(implicit val actorSystem: ActorSystem) {
  private val logger = Logging(actorSystem.eventStream, "tariff-service")
  private[charger] val tariffs = mutable.Map[Instant, Tariff]()

  def save(tariff: Tariff): TariffSaveResult = {
    if (!tariffs.contains(tariff.startDate)) {
      tariffs += (tariff.startDate -> tariff)
      logger.info(s"tariff saved, tariff=$tariff")
      TariffSaveResult(success = true, None)
    } else {
      val existingTariff = tariffs(tariff.startDate)
      logger.info(s"tariff not saved, reason=duplicate-start-date, existing-owner=${existingTariff.user.value} tariff=$tariff")
      TariffSaveResult(success = false, Some(s"tariff with the start date already exist. tariff owner=${existingTariff.user.value}"))
    }
  }

  def findTariff(date: Instant): Tariff = {
    val closestStartDate = tariffs.keySet.minBy { startDate =>
      val diff = date.getEpochSecond - startDate.getEpochSecond
      if (diff < 0) Int.MaxValue else diff
    }
    tariffs(closestStartDate)
  }

}
