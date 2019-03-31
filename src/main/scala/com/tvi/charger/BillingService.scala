package com.tvi.charger

import java.time.Instant

import akka.NotUsed
import akka.stream.alpakka.csv.scaladsl.CsvFormatting
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import com.tvi.charger.models._

import scala.collection.mutable

class BillingService(billingRepository: BillingRepository) {

  def saveAndCharge(session: ChargeSession, tariff: Tariff): ChargingBill = {
    val energyCost = computeSessionEnergyCost(session.energyConsumed, tariff.energyFee)
    val parkingCost = computeSessionParkingCost(session.startDate, session.endDate, tariff.parkingFee)
    val combinedCost = energyCost + parkingCost.getOrElse(0)
    val serviceCost = computeSessionServiceCost(combinedCost, tariff.serviceFee)
    val totalCost = energyCost + parkingCost.getOrElse(0) + serviceCost
    val chargingBill = ChargingBill(EnergyCost(energyCost), parkingCost.map(ParkingCost), ServiceCost(serviceCost), TotalChargingCost(totalCost), tariff.currency, tariff, session)
    billingRepository.saveChargingBill(chargingBill)
    chargingBill
  }

  def billingReportAsSource(user: User): Source[ByteString, NotUsed] = {
    val chargingBills = billingRepository.getUserBills(user)
    Source(chargingBills.toList)
      .via(chargingBillToValueMap)
      .via(chargingMapToValues)
      .prepend(Source.single(chargingBillKeys))
      .via(CsvFormatting.format(quoteChar = ' '))
  }

  private val chargingBillKeys = List("currency", "tariff energy fee", "tariff parking fee", "tariff service fee", "session start date", "session end date", "session energy consumed", "energy cost", "parking cost", "service cost", "total cost to pay")

  private def chargingBillToValueMap = Flow[ChargingBill].map { chargingBill =>
    mutable.LinkedHashMap[String, String](
      chargingBillKeys.head -> chargingBill.currency.getCurrencyCode,
      chargingBillKeys(1) -> chargingBill.tariff.energyFee.value.toString(),
      chargingBillKeys(2) -> chargingBill.tariff.parkingFee.map(_.value).getOrElse(BigDecimal(0)).toString(),
      chargingBillKeys(3) -> chargingBill.tariff.serviceFee.value.toString,
      chargingBillKeys(4) -> chargingBill.session.startDate.toString,
      chargingBillKeys(5) -> chargingBill.session.endDate.toString,
      chargingBillKeys(6) -> chargingBill.session.energyConsumed.value.toString,
      chargingBillKeys(7) -> chargingBill.energyCost.value.toString(),
      chargingBillKeys(8) -> chargingBill.parkingCost.map(_.value).getOrElse(BigDecimal(0)).toString(),
      chargingBillKeys(9) -> chargingBill.serviceCost.value.toString(),
      chargingBillKeys(10) -> chargingBill.totalChargingCost.value.toString(),
    )
  }

  private def chargingMapToValues = Flow[mutable.LinkedHashMap[String, String]].map { billMap =>
    billMap.values.toList
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
