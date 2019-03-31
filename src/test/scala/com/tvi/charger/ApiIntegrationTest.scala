package com.tvi.charger

import java.time.Instant
import java.util.Currency

import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.tvi.charger.models._
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import play.api.libs.json.Json

class ApiIntegrationTest extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterEach {
  private val apiConfig = ApiConfig(testConfig, system)
  private val billingRepository = new BillingRepository
  private val tariffRepository = new TariffRepository
  private val billingService = new BillingService(billingRepository)
  private val tariffService = new TariffService(tariffRepository)

  private val apiInTest = new Api(apiConfig, tariffService, billingService)

  override protected def beforeEach(): Unit = {
    fixture.cleanApplicationState()
  }

  "Charger Api" should {

    "save tariff with authenticated requests" in {

      val tariffJsonWithUser =
        """
          |{
          |  "energyFee": 1,
          |  "parkingFee": 1,
          |  "serviceFee": 0.1,
          |  "currency": "EUR",
          |  "startDate": "2020-01-19T16:30:50Z",
          |  "user": "ismet"
          |}
        """.stripMargin

      Post("/tariffs", HttpEntity(ContentTypes.`application/json`, fixture.tariffJson)) ~>
        addCredentials(HttpCredentials.createBasicHttpCredentials("ismet", "password")) ~>
        Route.seal(apiInTest.route) ~> check {
        Json.parse(responseAs[String]) shouldEqual Json.parse(tariffJsonWithUser)
        status shouldEqual StatusCodes.OK
        contentType shouldEqual ContentTypes.`application/json`
      }
    }

    "reject tariff requests w/o auth info at headers" in {
      Post("/tariffs", HttpEntity(ContentTypes.`application/json`, fixture.tariffJson)) ~>
        Route.seal(apiInTest.route) ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }

    "ignore user info incoming in body of tariff but enrich with header user info" in {
      val tariffJsonWithHackyUser =
        """
          |{
          |  "energyFee": 1,
          |  "parkingFee": 1,
          |  "serviceFee": 0.1,
          |  "currency": "EUR",
          |  "startDate": "2020-01-19T16:30:50Z",
          |  "user": "hacky-user-name"
          |}
        """.stripMargin

      val tariffJsonWithCorrectUser =
        """
          |{
          |  "energyFee": 1,
          |  "parkingFee": 1,
          |  "serviceFee": 0.1,
          |  "currency": "EUR",
          |  "startDate": "2020-01-19T16:30:50Z",
          |  "user": "ismet"
          |}
        """.stripMargin

      Post("/tariffs", HttpEntity(ContentTypes.`application/json`, tariffJsonWithHackyUser)) ~>
        addCredentials(HttpCredentials.createBasicHttpCredentials("ismet", "password")) ~>
        Route.seal(apiInTest.route) ~> check {
        Json.parse(responseAs[String]) shouldEqual Json.parse(tariffJsonWithCorrectUser)
        status shouldEqual StatusCodes.OK
        contentType shouldEqual ContentTypes.`application/json`
      }
    }

    "forbid duplicate tariffs with same start date" in {
      Post("/tariffs", HttpEntity(ContentTypes.`application/json`, fixture.tariffJson)) ~>
        addCredentials(HttpCredentials.createBasicHttpCredentials("ismet", "password")) ~>
        Route.seal(apiInTest.route) ~> check {
        status shouldEqual StatusCodes.OK
      }

      Post("/tariffs", HttpEntity(ContentTypes.`application/json`, fixture.tariffJson)) ~>
        addCredentials(HttpCredentials.createBasicHttpCredentials("ismet", "password")) ~>
        Route.seal(apiInTest.route) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "save charging session" in {
      fixture.saveSampleTariff()

      Post("/sessions", HttpEntity(ContentTypes.`application/json`, fixture.chargeSessionJson)) ~>
        addHeader("X-User", "ismet") ~>
        Route.seal(apiInTest.route) ~> check {
        Json.parse(responseAs[String]) shouldEqual Json.parse(fixture.chargingBillJson)
        status shouldEqual StatusCodes.OK
        contentType shouldEqual ContentTypes.`application/json`
      }
    }

    "ignore user info incoming in body of session but enrich with header user info" in {
      fixture.saveSampleTariff()

      val chargeSessionWithHackyUser =
        """
          |{
          |  "user":"hacky-user-info",
          |  "startDate": "2010-01-19T16:30:50Z",
          |  "endDate": "2010-01-20T16:30:50Z",
          |  "energyConsumed":1
          |}
        """.stripMargin

      Post("/sessions", HttpEntity(ContentTypes.`application/json`, chargeSessionWithHackyUser)) ~>
        addHeader("X-User", "ismet") ~>
        Route.seal(apiInTest.route) ~> check {
        Json.parse(responseAs[String]) shouldEqual Json.parse(fixture.chargingBillJson)
        status shouldEqual StatusCodes.OK
        contentType shouldEqual ContentTypes.`application/json`
      }
    }

    "report user sessions in csv" in {
      fixture.saveSampleBill()
      Get("/sessions") ~>
        addHeader("X-User", "ismet") ~>
        Route.seal(apiInTest.route) ~> check {
        val csvLines = chunks.toList.map(_.data().utf8String)
        status shouldEqual StatusCodes.OK
        contentType shouldEqual ContentTypes.`text/csv(UTF-8)`
        headers.contains(RawHeader("Content-Disposition", s"attachment; filename=billing-report.csv")) shouldEqual true
        csvLines.head shouldEqual "currency, tariff  energy  fee , tariff  parking  fee , tariff  service  fee , session  start  date , session  end  date , session  energy  consumed , energy  cost , parking  cost , service  cost , total  cost  to  pay \r\n"
        csvLines(1) shouldEqual "EUR,1,1,0.1,2010-01-19T16:30:50Z,2010-01-20T16:30:50Z,1.0,1,24.0,2.5,27.5\r\n"
      }

    }
  }

  object fixture {
    val tariffJson: String =
      """
        |{
        |  "energyFee": 1,
        |  "parkingFee": 1,
        |  "serviceFee": 0.1,
        |  "currency": "EUR",
        |  "startDate": "2020-01-19T16:30:50Z"
        |}
      """.stripMargin

    val chargeSessionJson: String =
      """
        |{
        |  "user":"ismet",
        |  "startDate": "2010-01-19T16:30:50Z",
        |  "endDate": "2010-01-20T16:30:50Z",
        |  "energyConsumed":1
        |}
      """.stripMargin

    val chargingBillJson: String =
      """
        |{
        |  "totalChargingCost": 27.5,
        |  "serviceCost": 2.5,
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
        |  "parkingCost": 24,
        |  "energyCost": 1
        |}
      """.stripMargin

    private val sampleTariff = Tariff(
      energyFee = EnergyFee(1),
      parkingFee = Some(ParkingFee(1)),
      serviceFee = ServiceFee(0.1),
      currency = Currency.getInstance("EUR"),
      startDate = Instant.parse("2020-01-19T16:30:50Z"),
      user = User("ismet")
    )

    def cleanApplicationState(): Unit = {
      tariffRepository.tariffs.clear()
      billingRepository.chargingBillsByUser.clear()
      billingRepository.chargingBillsToInvoice.clear()
    }

    def saveSampleTariff(): Unit = {
      tariffRepository.tariffs += (Instant.MIN -> sampleTariff)
    }

    def saveSampleBill(): Unit = {
      saveSampleTariff()

      val chargeSession = ChargeSession(
        user = User("ismet"),
        startDate = Instant.parse("2010-01-19T16:30:50Z"),
        endDate = Instant.parse("2010-01-20T16:30:50Z"),
        energyConsumed = EnergyKwh(1)
      )

      val chargingBill = ChargingBill(
        energyCost = EnergyCost(1),
        parkingCost = Some(ParkingCost(24.0)),
        serviceCost = ServiceCost(2.50),
        totalChargingCost = TotalChargingCost(27.50),
        currency = Currency.getInstance("EUR"),
        tariff = sampleTariff,
        session = chargeSession
      )

      billingRepository.chargingBillsByUser += (chargeSession.user -> Seq(chargingBill))
    }
  }

}
