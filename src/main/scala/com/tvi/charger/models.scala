package com.tvi.charger

import java.time.Instant
import java.util.Currency

import play.api.libs.json._

object models {

  case class Tariff(energyFee: EnergyFee, parkingFee: Option[ParkingFee], serviceFee: ServiceFee, currency: Currency, startDate: Instant)

  case class EnergyFee(value: BigDecimal) {
    require(value >= 0, "energy fee should be >= 0")
  }

  case class ParkingFee(value: BigDecimal) {
    require(value >= 0, "parking fee should be >= 0")
  }

  case class ServiceFee(value: Float) {
    require(value > 0.0f && value <= 0.5f, "service fee should be between 0.1 and 0.5")
  }

  object codecs {

    implicit val engReads: Reads[EnergyFee] = __.read[BigDecimal].map { curr => EnergyFee.apply(curr) }
    implicit val engWrites: Writes[EnergyFee] = __.write[BigDecimal].contramap { curr: EnergyFee => curr.value }
    implicit val energyFormat: Format[EnergyFee] = Format(engReads, engWrites)

    implicit val parkingReads: Reads[ParkingFee] = __.read[BigDecimal].map { curr => ParkingFee.apply(curr) }
    implicit val parkingWrites: Writes[ParkingFee] = __.write[BigDecimal].contramap { curr: ParkingFee => curr.value }
    implicit val parkingFormat: Format[ParkingFee] = Format(parkingReads, parkingWrites)

    implicit val serviceReads: Reads[ServiceFee] = __.read[Float].map { curr => ServiceFee.apply(curr) }
    implicit val serviceWrites: Writes[ServiceFee] = __.write[Float].contramap { curr: ServiceFee => curr.value }
    implicit val serviceFormat: Format[ServiceFee] = Format(serviceReads, serviceWrites)

    implicit val currReads: Reads[Currency] = __.read[String].map { curr => Currency.getInstance(curr) }
    implicit val currWrites: Writes[Currency] = __.write[String].contramap { curr: Currency => curr.getCurrencyCode }
    implicit val currFmt: Format[Currency] = Format(currReads, currWrites)

    implicit val tariffFormat: Format[Tariff] = Json.format[Tariff]
  }

}
