package com.tvi.charger

import java.time.Instant

import com.tvi.charger.models.Tariff

import scala.collection.mutable

case class TariffSaveResult(success: Boolean, reason: Option[String])

class TariffService {
  private val tariffs = mutable.Map[Instant, Tariff]()

  def save(tariff: Tariff): TariffSaveResult = {
    if (!tariffs.contains(tariff.startDate)) {
      tariffs += (tariff.startDate -> tariff)
      TariffSaveResult(success = true, None)
    } else {
      TariffSaveResult(success = false, Some(s"tariff with the start date already exist. tariff owner=${tariff.user}"))
    }

  }

}
