package com.tvi.charger

import java.time.Instant

import akka.actor.ActorSystem
import akka.event.Logging
import com.tvi.charger.models._
import play.api.libs.json.Json

import scala.collection.mutable
import models.codecs._

case class ChargeCostResult(totalCost: BigDecimal, tariff: Tariff)

class ChargeSessionService()(implicit val actorSystem: ActorSystem) {
  private val logger = Logging(actorSystem.eventStream, "charge-session-service")
  private val sessions = mutable.Map[User, Seq[ChargeSession]]()

  def save(session: ChargeSession): Unit = {
    if (sessions.contains(session.user)) {
      sessions(session.user) = sessions(session.user) :+ session
    } else {
      sessions(session.user) = Seq(session)
    }
    logger.info(s"charge session saved, session=${Json.toJson(session).toString()}")
  }

  def charge(session: ChargeSession, tariff: Tariff) = {
    val energyCost = computeSessionEnergyCost(session.energyConsumed, tariff.energyFee)
    val parkingCost = computeSessionParkingCost(session.startDate, session.endDate, tariff.parkingFee)
    val combinedCost = energyCost + parkingCost.getOrElse(0)
    val serviceCost = computeSessionServiceCost(combinedCost, tariff.serviceFee)
    val totalCost = energyCost + parkingCost.getOrElse(0) + serviceCost
    ChargeCostResult(totalCost, tariff)
  }

  private def computeSessionEnergyCost(energyKwh: EnergyKwh, energyFee: EnergyFee): BigDecimal = {
    energyFee.value * energyKwh.value
  }

  private def computeSessionParkingCost(startDate: Instant, endDate: Instant, parkingFee: Option[ParkingFee]): Option[BigDecimal] = {
    parkingFee.map { fee =>
      val durationInSeconds = endDate.getEpochSecond - startDate.getEpochSecond
      val durationInHours = Math.ceil(durationInSeconds.toDouble / (60 * 60).toDouble)
      fee.value * durationInHours
    }
  }

  private def computeSessionServiceCost(calculatedCost: BigDecimal, serviceFee: ServiceFee) = {
    calculatedCost * serviceFee.value
  }
}
