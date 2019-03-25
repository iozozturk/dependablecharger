package com.tvi.charger

import akka.actor.ActorSystem
import com.typesafe.config.Config

case class ApiConfig(host: String, port: Int)

object ApiConfig {

  def apply(config: Config, system: ActorSystem): ApiConfig =
    new ApiConfig(
      config.getString("service.host"),
      config.getInt("service.port")
    )
}
