package com.tvi.charger

import java.time.Instant
import java.util.Currency

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.tvi.charger.models._
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class BillingServiceTest extends WordSpec with Matchers with MockitoSugar {
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
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

    "report billing of user as a csv source" in {
      when(mockBillingRepo.getUserBills(chargeSession.user)) thenReturn Seq(chargingBill)

      val userReportSource = billingServiceInTest.billingReportAsSource(chargeSession.user)
      val csvLinesInBytes = Await.result(userReportSource.take(2).runWith(Sink.seq), 3.second)
      val csvLines = csvLinesInBytes.map(_.utf8String)
      csvLines.head shouldEqual "currency, tariff  energy  fee , tariff  parking  fee , tariff  service  fee , session  start  date , session  end  date , session  energy  consumed , energy  cost , parking  cost , service  cost , total  cost  to  pay \r\n"
      csvLines(1) shouldEqual "EUR,1,1,0.1,2010-01-19T16:30:50Z,2010-01-20T16:30:50Z,1.0,1,24.0,2.5,27.5\r\n"
    }


  }

}
