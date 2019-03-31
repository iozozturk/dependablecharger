package com.tvi.charger

import java.time.Instant
import java.util.Currency

import com.tvi.charger.models._
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

class BillingRepositoryTest extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {
  val billingRepoInTest = new BillingRepository()

  override protected def beforeEach(): Unit = {
    fixture.cleanBillingState()
  }

  "TariffRepositoryTest" should {

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
      parkingCost = Some(ParkingCost(1)),
      serviceCost = ServiceCost(1),
      totalChargingCost = TotalChargingCost(1),
      currency = Currency.getInstance("EUR"),
      tariff = tariff,
      session = chargeSession
    )

    "save new charging bill" in {
      billingRepoInTest.saveChargingBill(chargingBill) shouldEqual BillingSaveResult(success = true,None)
      billingRepoInTest.chargingBillsToInvoice.size shouldEqual 1
      billingRepoInTest.chargingBillsToInvoice.head shouldEqual chargingBill

      billingRepoInTest.chargingBillsByUser.size shouldEqual 1
      billingRepoInTest.chargingBillsByUser.values.head.head shouldEqual chargingBill
    }

    "save subsequent new charging bills to update existing user bills" in {
      billingRepoInTest.saveChargingBill(chargingBill) shouldEqual BillingSaveResult(success = true,None)
      billingRepoInTest.saveChargingBill(chargingBill) shouldEqual BillingSaveResult(success = true,None)
      billingRepoInTest.chargingBillsToInvoice.size shouldEqual 2
      billingRepoInTest.chargingBillsToInvoice.head shouldEqual chargingBill

      billingRepoInTest.chargingBillsByUser.size shouldEqual 1
      billingRepoInTest.chargingBillsByUser.values.size shouldEqual 1
      billingRepoInTest.chargingBillsByUser.values.head.size shouldEqual 2
    }

    "get user bill" in {
      billingRepoInTest.saveChargingBill(chargingBill) shouldEqual BillingSaveResult(success = true,None)
      billingRepoInTest.getUserBills(chargeSession.user).size shouldEqual 1
      billingRepoInTest.getUserBills(chargeSession.user).head shouldEqual chargingBill
    }

    "respond with empty list for non existing user bills" in {
      billingRepoInTest.getUserBills(User("not-exists")) shouldEqual Seq.empty
    }


  }

  object fixture {
    def cleanBillingState(): Unit = {
      billingRepoInTest.chargingBillsByUser.clear()
      billingRepoInTest.chargingBillsToInvoice.clear()
    }
  }

}
