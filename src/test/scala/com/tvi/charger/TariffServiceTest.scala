package com.tvi.charger

import java.time.Instant
import java.util.Currency

import akka.actor.ActorSystem
import com.tvi.charger.models._
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

class TariffServiceTest extends WordSpec with Matchers with MockitoSugar {
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private val mockTariffRepo = mock[TariffRepository]
  private val tariffServiceInTest = new TariffService(mockTariffRepo)

  "TariffServiceTest" should {

    "save new tariff" in {
      val tariff = Tariff(EnergyFee(1), Some(ParkingFee(1)), ServiceFee(0.1), Currency.getInstance("EUR"), Instant.parse("2119-03-27T00:00:50Z"), User("ismet"))
      val saveResult = TariffSaveResult(success = true, None)

      when(mockTariffRepo.save(tariff)) thenReturn saveResult
      tariffServiceInTest.save(tariff) shouldEqual saveResult
    }

    "get tariff for the time" in{
      val now = Instant.now
      val tariff = Tariff(EnergyFee(1), Some(ParkingFee(1)), ServiceFee(0.1), Currency.getInstance("EUR"), Instant.parse("2119-03-27T00:00:50Z"), User("ismet"))

      when(mockTariffRepo.getClosestTariff(now)) thenReturn tariff
      tariffServiceInTest.getTariff(now) shouldEqual tariff
    }


  }

}
