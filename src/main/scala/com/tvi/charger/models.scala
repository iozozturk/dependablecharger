package com.tvi.charger

import play.api.libs.json.Json

case class Tariff(id: String)

object Tariff {
  implicit val tariffFormat = Json.format[Tariff]
}
