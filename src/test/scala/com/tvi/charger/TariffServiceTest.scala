package com.tvi.charger

import java.time.Instant
import java.util.Currency

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.tvi.charger.models._
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

class TariffServiceTest extends WordSpec with Matchers with MockitoSugar {
  implicit val actorSystem: ActorSystem = ActorSystem("dress-pipeline")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "TariffServiceTest" should {
    val tariffServiceInTest = new TariffService()

    "save" in {

    }

    "find closes tariff to date" in {
      Seq(
        Tariff(EnergyFee(1), Some(ParkingFee(1)), ServiceFee(0.1), Currency.getInstance("EUR"), Instant.parse("2119-03-27T00:00:50Z"), User("ismet")),
        Tariff(EnergyFee(2), Some(ParkingFee(2)), ServiceFee(0.2), Currency.getInstance("EUR"), Instant.parse("2119-03-25T00:00:50Z"), User("merve")),
        Tariff(EnergyFee(3), Some(ParkingFee(3)), ServiceFee(0.3), Currency.getInstance("EUR"), Instant.parse("2119-03-23T00:00:50Z"), User("elon")),
      ).foreach { tariff =>
        tariffServiceInTest.tariffs += (tariff.startDate -> tariff)
      }

      val tariff = tariffServiceInTest.findTariff(Instant.parse("2119-03-26T00:00:50Z"))
      tariff.startDate shouldEqual Instant.parse("21k19-03-25T00:00:50Z")
    }
  }
}
