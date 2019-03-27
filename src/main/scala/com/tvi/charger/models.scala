package com.tvi.charger

import java.time.Instant
import java.util.Currency

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

object models {

  case class Tariff(energyFee: EnergyFee, parkingFee: Option[ParkingFee], serviceFee: ServiceFee, currency: Currency, startDate: Instant, user: User) {
    require(startDate.getEpochSecond >= Instant.now().getEpochSecond, "tariff start date cannot be a past date")
    require(energyFee.value >= 0, "energy fee should be >= 0")
    require(parkingFee.map(_.value).getOrElse(BigDecimal(0)) >= 0, "parking fee should be >= 0")
    require(serviceFee.value > 0.0 && serviceFee.value <= 0.5, "service fee should be between 0.1 and 0.5")
  }

  case class ChargeSession(user: User, startDate: Instant, endDate: Instant, energyConsumed: EnergyKwh) {
    require(startDate.toEpochMilli <= Instant.now().toEpochMilli, "start date must be in the past")
    require(endDate.toEpochMilli <= Instant.now().toEpochMilli, "end date must be in the past")
  }

  case class EnergyFee(value: BigDecimal) extends AnyVal

  case class ParkingFee(value: BigDecimal) extends AnyVal

  case class ServiceFee(value: Double) extends AnyVal

  case class User(value: String) extends AnyVal

  case class EnergyKwh(value: Int) extends AnyVal

  object codecs {

    implicit val tariffWrites: Writes[Tariff] = (tariff: Tariff) => Json.obj(
      "energyFee" -> tariff.energyFee.value,
      "serviceFee" -> tariff.serviceFee.value,
      "currency" -> tariff.currency.getCurrencyCode,
      "startDate" -> tariff.startDate,
      "user" -> tariff.user.value,
    ) ++ tariff.parkingFee.map(fee => Json.obj("parkingFee" -> fee.value)).getOrElse(Json.obj())

    implicit val tariffReads: Reads[Tariff] = (
      (JsPath \ "energyFee").read[BigDecimal].map(EnergyFee) and
        (JsPath \ "parkingFee").readNullable[BigDecimal].map(_.map(ParkingFee)) and
        (JsPath \ "serviceFee").read[Double].map(ServiceFee) and
        (JsPath \ "currency").read[String].map(Currency.getInstance) and
        (JsPath \ "startDate").read[Instant]
      ) (Tariff.apply(_, _, _, _, _, User("")))

    implicit val sessionWrites: Writes[ChargeSession] = (session: ChargeSession) => Json.obj(
      "user" -> session.user.value,
      "startDate" -> session.startDate,
      "endDate" -> session.endDate,
      "energyConsumed" -> session.energyConsumed.value
    )

    implicit val sessionReads: Reads[ChargeSession] = (
      (JsPath \ "user").read[BigDecimal].map(EnergyFee) and
        (JsPath \ "startDate").read[Instant] and
        (JsPath \ "endDate").read[Instant] and
        (JsPath \ "energyConsumed").read[Int].map(EnergyKwh)
      ) (ChargeSession.apply(_, _, _, _))
  }

}
