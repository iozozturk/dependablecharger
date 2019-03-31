package com.tvi.charger

import java.time.Instant
import java.util.Currency

import com.tvi.charger.models._
import com.tvi.charger.models.codecs._
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json

class ModelsTest extends WordSpec with Matchers with MockitoSugar {

  "Tariff model" should {
    val tariff = Tariff(
      energyFee = EnergyFee(1),
      parkingFee = Some(ParkingFee(1)),
      serviceFee = ServiceFee(0.1),
      currency = Currency.getInstance("EUR"),
      startDate = Instant.parse("2020-01-19T16:30:50Z"),
      user = User("ismet")
    )

    val tariffJson =
      """
        |{
        |  "energyFee": 1,
        |  "parkingFee": 1,
        |  "serviceFee": 0.1,
        |  "currency": "EUR",
        |  "startDate": "2020-01-19T16:30:50Z",
        |  "user":"ismet"
        |}
      """.stripMargin

    "serialize into json" in {
      Json.toJson(tariff) shouldEqual Json.parse(tariffJson)
    }

    "deserialize from json ignoring user name" in {
      Json.parse(tariffJson).as[Tariff] shouldEqual tariff.copy(user = User(""))
    }

    "throw exception when start date is in the past" in {
      the[IllegalArgumentException] thrownBy Tariff(
        energyFee = EnergyFee(1),
        parkingFee = Some(ParkingFee(1)),
        serviceFee = ServiceFee(0.1),
        currency = Currency.getInstance("EUR"),
        startDate = Instant.parse("1020-01-19T16:30:50Z"),
        user = User("ismet")
      )
    }

    "throw exception when energy fee is negative" in {
      the[IllegalArgumentException] thrownBy Tariff(
        energyFee = EnergyFee(-1),
        parkingFee = Some(ParkingFee(1)),
        serviceFee = ServiceFee(0.1),
        currency = Currency.getInstance("EUR"),
        startDate = Instant.parse("2120-01-19T16:30:50Z"),
        user = User("ismet")
      )
    }

    "throw exception when parking fee is negative" in {
      the[IllegalArgumentException] thrownBy Tariff(
        energyFee = EnergyFee(1),
        parkingFee = Some(ParkingFee(-1)),
        serviceFee = ServiceFee(0.1),
        currency = Currency.getInstance("EUR"),
        startDate = Instant.parse("2120-01-19T16:30:50Z"),
        user = User("ismet")
      )
    }

    "throw exception when service fee is not between 0.1 and 0.5" in {
      the[IllegalArgumentException] thrownBy Tariff(
        energyFee = EnergyFee(1),
        parkingFee = Some(ParkingFee(1)),
        serviceFee = ServiceFee(0.6),
        currency = Currency.getInstance("EUR"),
        startDate = Instant.parse("2120-01-19T16:30:50Z"),
        user = User("ismet")
      )
    }
  }

  "ChargeSession model" should {
    val chargeSession = ChargeSession(
      user = User("ismet"),
      startDate = Instant.parse("2010-01-19T16:30:50Z"),
      endDate = Instant.parse("2010-01-20T16:30:50Z"),
      energyConsumed = EnergyKwh(1)
    )
    val chargeSessionWithEmptyUser = ChargeSession(
      user = User(""),
      startDate = Instant.parse("2010-01-19T16:30:50Z"),
      endDate = Instant.parse("2010-01-20T16:30:50Z"),
      energyConsumed = EnergyKwh(1)
    )

    val chargeSessionJson =
      """
        |{
        |  "user":"ismet",
        |  "startDate": "2010-01-19T16:30:50Z",
        |  "endDate": "2010-01-20T16:30:50Z",
        |  "energyConsumed":1
        |}
      """.stripMargin

    "serialize into json" in {
      Json.toJson(chargeSession) shouldEqual Json.parse(chargeSessionJson)
    }
    "deserialize from json skipping user info" in {
      Json.parse(chargeSessionJson).as[ChargeSession] shouldEqual chargeSessionWithEmptyUser
    }

    "throw exception when start date is in the future" in {
      the[IllegalArgumentException] thrownBy ChargeSession(
        user = User("ismet"),
        startDate = Instant.parse("2050-01-19T16:30:50Z"),
        endDate = Instant.parse("2010-01-20T16:30:50Z"),
        energyConsumed = EnergyKwh(1)
      )
    }

    "throw exception when energy consumed is negative" in {
      the[IllegalArgumentException] thrownBy ChargeSession(
        user = User("ismet"),
        startDate = Instant.parse("2010-01-19T16:30:50Z"),
        endDate = Instant.parse("2010-01-20T16:30:50Z"),
        energyConsumed = EnergyKwh(-1)
      )
    }

    "throw exception when end date is smaller than start date" in {
      the[IllegalArgumentException] thrownBy ChargeSession(
        user = User("ismet"),
        startDate = Instant.parse("2010-01-19T16:30:50Z"),
        endDate = Instant.parse("2010-01-10T16:30:50Z"),
        energyConsumed = EnergyKwh(-1)
      )
    }
  }

  "ChargingBill model" should {
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

    val chargingBillJson =
      """
        |{
        |  "totalChargingCost": 1,
        |  "serviceCost": 1,
        |  "session": {
        |    "user": "ismet",
        |    "startDate": "2010-01-19T16:30:50Z",
        |    "endDate": "2010-01-20T16:30:50Z",
        |    "energyConsumed": 1
        |  },
        |  "currency": "EUR",
        |  "tariff": {
        |    "serviceFee": 0.1,
        |    "parkingFee": 1,
        |    "currency": "EUR",
        |    "energyFee": 1,
        |    "user": "ismet",
        |    "startDate": "2020-01-19T16:30:50Z"
        |  },
        |  "parkingCost": 1,
        |  "energyCost": 1
        |}
      """.stripMargin

    "serialize into json" in {
      Json.toJson(chargingBill) shouldEqual Json.parse(chargingBillJson)
    }
  }


}
