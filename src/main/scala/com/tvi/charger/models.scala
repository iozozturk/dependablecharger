package com.tvi.charger

import java.time.Instant
import java.util.Currency

import play.api.libs.json._
import play.api.libs.functional.syntax.toFunctionalBuilderOps

object models {

  case class Tariff(energyFee: EnergyFee, parkingFee: Option[ParkingFee], serviceFee: ServiceFee, currency: Currency, startDate: Instant, user:String)

  case class EnergyFee(value: BigDecimal) {
    require(value >= 0, "energy fee should be >= 0")
  }

  case class ParkingFee(value: BigDecimal) {
    require(value >= 0, "parking fee should be >= 0")
  }

  case class ServiceFee(value: Double) {
    require(value > 0.0 && value <= 0.5, "service fee should be between 0.1 and 0.5")
  }

  object codecs {
    implicit val tariffWrites: Writes[Tariff] = (tariff: Tariff) => Json.obj(
      "energyFee" -> tariff.energyFee.value,
      "serviceFee" -> tariff.serviceFee.value,
      "currency" -> tariff.currency.getCurrencyCode,
      "startDate" -> tariff.startDate,
      "user" -> tariff.user,
    ) ++ tariff.parkingFee.map(fee => Json.obj("parkingFee" -> fee.value)).getOrElse(Json.obj())

    implicit val tariffReads: Reads[Tariff] = (
      (JsPath \ "energyFee").read[BigDecimal].map(EnergyFee) and
        (JsPath \ "parkingFee").readNullable[BigDecimal].map(_.map(ParkingFee)) and
        (JsPath \ "serviceFee").read[Double].map(ServiceFee) and
        (JsPath \ "currency").read[String].map(Currency.getInstance) and
        (JsPath \ "startDate").read[Instant]
      ) (Tariff.apply(_: EnergyFee, _: Option[ParkingFee], _: ServiceFee, _: Currency, _: Instant, ""))
  }

}
