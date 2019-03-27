package com.tvi.charger

import akka.actor.ActorSystem
import akka.event.Logging
import com.tvi.charger.models.{ChargeSession, Tariff, User}

import scala.collection.mutable

class ChargeSessionService()(implicit val actorSystem: ActorSystem) {
  private val logger = Logging(actorSystem.eventStream, "charger-api")
  private val sessions = mutable.Map[User, Seq[ChargeSession]]()

  def save(session: ChargeSession): Unit = {
    if (sessions.contains(session.user)) {
      sessions(session.user) = sessions(session.user) :+ session
    } else {
      sessions(session.user) = Seq(session)
    }
    logger.info(s"charge session saved, session=$session")
  }

  def charge(session:ChargeSession, tariff:Tariff)={

  }

}
