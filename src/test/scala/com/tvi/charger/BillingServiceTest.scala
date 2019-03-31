package com.tvi.charger

import java.time.Instant
import java.util.Currency

import akka.actor.ActorSystem
import com.tvi.charger.models._
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

class BillingServiceTest extends WordSpec with Matchers with MockitoSugar {
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private val mockBillingRepo = mock[BillingRepository]
  private val billingServiceInTest = new BillingService(mockBillingRepo)

  "Billing Service" should {
    val chargeSession = ChargeSession(
      user = User("ismet"),
      startDate = Instant.parse("2010-01-19T16:30:50Z"),
      endDate = Instant.parse("2010-01-20T16:30:50Z"),
      energyConsumed = EnergyKwh(1)
    )

    val tariff = Tariff(
      energyFee = EnergyFee(1),
      parkingFee = Some(ParkingFee(1)),
      serviceFee = ServiceFee(0.1),
      currency = Currency.getInstance("EUR"),
      startDate = Instant.parse("2020-01-19T16:30:50Z"),
      user = User("ismet")
    )

    val chargingBill = ChargingBill(
      energyCost = EnergyCost(1),
      parkingCost = Some(ParkingCost(24.0)),
      serviceCost = ServiceCost(2.50),
      totalChargingCost = TotalChargingCost(27.50),
      currency = Currency.getInstance("EUR"),
      tariff = tariff,
      session = chargeSession
    )

    "save and charge new session" in {
      when(mockBillingRepo.saveChargingBill(chargingBill)) thenReturn BillingSaveResult(success = true, None)
      billingServiceInTest.saveAndCharge(chargeSession, tariff) shouldEqual chargingBill
    }

    "save charging bill parking cost with seconds precision" in {
      val tenSecondParking = chargeSession.copy(endDate = Instant.parse("2010-01-20T16:31:20Z"))
      val billWithTenSecondParking = chargingBill.copy(session = tenSecondParking,
        parkingCost = Some(ParkingCost(BigDecimal(24.01))),
        totalChargingCost = TotalChargingCost(BigDecimal(27.51))
      )
      when(mockBillingRepo.saveChargingBill(billWithTenSecondParking)) thenReturn BillingSaveResult(success = true, None)
      billingServiceInTest.saveAndCharge(tenSecondParking, tariff) shouldEqual billWithTenSecondParking
    }


  }

}
