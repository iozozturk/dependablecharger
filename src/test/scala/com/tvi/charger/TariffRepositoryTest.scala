package com.tvi.charger

import java.time.Instant
import java.util.Currency

import com.tvi.charger.models._
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

class TariffRepositoryTest extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {
  val tariffRepoInTest = new TariffRepository()

  override protected def beforeEach(): Unit = {
    fixture.cleanTariffState()
  }

  "TariffRepositoryTest" should {

    "save new tariff" in {
      val tariff = Tariff(EnergyFee(1), Some(ParkingFee(1)), ServiceFee(0.1), Currency.getInstance("EUR"), Instant.parse("2119-03-27T00:00:50Z"), User("ismet"))
      tariffRepoInTest.save(tariff) shouldEqual TariffSaveResult(success = true, None)
    }

    "respond with not saved for new tariff with duplicate existing startdate" in {
      val tariff = Tariff(EnergyFee(1), Some(ParkingFee(1)), ServiceFee(0.1), Currency.getInstance("EUR"), Instant.parse("2119-03-27T00:00:50Z"), User("ismet"))
      tariffRepoInTest.tariffs += (tariff.startDate -> tariff)
      val tariff2 = Tariff(EnergyFee(1), Some(ParkingFee(1)), ServiceFee(0.1), Currency.getInstance("EUR"), Instant.parse("2119-03-27T00:00:50Z"), User("ismet"))
      tariffRepoInTest.save(tariff2) shouldEqual TariffSaveResult(success = false, Some("tariff with the start date already exist. tariff owner=ismet"))
    }

    "find closest tariff to date" in {
      Seq(
        Tariff(EnergyFee(1), Some(ParkingFee(1)), ServiceFee(0.1), Currency.getInstance("EUR"), Instant.parse("2119-03-27T00:00:50Z"), User("ismet")),
        Tariff(EnergyFee(2), Some(ParkingFee(2)), ServiceFee(0.2), Currency.getInstance("EUR"), Instant.parse("2119-03-25T00:00:50Z"), User("ismet")),
        Tariff(EnergyFee(3), Some(ParkingFee(3)), ServiceFee(0.3), Currency.getInstance("EUR"), Instant.parse("2119-03-23T00:00:50Z"), User("ismet")),
        Tariff(EnergyFee(3), Some(ParkingFee(3)), ServiceFee(0.3), Currency.getInstance("EUR"), Instant.parse("2119-03-21T00:00:50Z"), User("ismet")),
        Tariff(EnergyFee(3), Some(ParkingFee(3)), ServiceFee(0.3), Currency.getInstance("EUR"), Instant.parse("2119-03-19T00:00:50Z"), User("ismet")),
        Tariff(EnergyFee(3), Some(ParkingFee(3)), ServiceFee(0.3), Currency.getInstance("EUR"), Instant.parse("2119-03-19T00:00:49Z"), User("ismet")),
      ).foreach { tariff =>
        tariffRepoInTest.tariffs += (tariff.startDate -> tariff)
      }

      val tariff = tariffRepoInTest.getClosestTariff(Instant.parse("2119-03-26T00:00:50Z"))
      tariff.startDate shouldEqual Instant.parse("2119-03-25T00:00:50Z")

      val tariff2 = tariffRepoInTest.getClosestTariff(Instant.parse("2119-03-20T00:00:50Z"))
      tariff2.startDate shouldEqual Instant.parse("2119-03-19T00:00:50Z")

      val tariff3 = tariffRepoInTest.getClosestTariff(Instant.parse("2119-03-30T00:00:50Z"))
      tariff3.startDate shouldEqual Instant.parse("2119-03-27T00:00:50Z")

      val tariff4 = tariffRepoInTest.getClosestTariff(Instant.parse("2119-03-19T00:00:49Z"))
      tariff4.startDate shouldEqual Instant.parse("2119-03-19T00:00:49Z")
    }

  }

  object fixture {
    def cleanTariffState(): Unit = {
      tariffRepoInTest.tariffs.clear()
    }
  }

}
