package com.tvi.charger

import java.time.Instant

import com.tvi.charger.models.Tariff

import scala.collection.mutable

case class TariffSaveResult(success: Boolean, reason: Option[String])

class TariffRepository {
  private[charger] val tariffs = mutable.Map[Instant, Tariff]()

  def save(tariff: Tariff): TariffSaveResult = {
    if (!tariffs.contains(tariff.startDate)) {
      tariffs += (tariff.startDate -> tariff)
      TariffSaveResult(success = true, None)
    } else {
      val existingTariff = tariffs(tariff.startDate)
      TariffSaveResult(success = false, Some(s"tariff with the start date already exist. tariff owner=${existingTariff.user.value}"))
    }
  }

  def getClosestTariff(date: Instant): Tariff = {
    val closestStartDate = tariffs.keySet.minBy { startDate =>
      val diff = date.getEpochSecond - startDate.getEpochSecond
      if (diff < 0) Int.MaxValue else diff
    }
    tariffs(closestStartDate)
  }

}
