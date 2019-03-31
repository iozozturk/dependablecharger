package com.tvi.charger

import com.tvi.charger.models.{ChargingBill, User}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class BillingRepository {
  private[charger] val chargingBillsByUser = mutable.Map[User, Seq[ChargingBill]]()
  private[charger] val chargingBillsToInvoice = ListBuffer[ChargingBill]()

  def saveChargingBill(chargingBill: ChargingBill): Unit = {
    val user = chargingBill.session.user
    if (chargingBillsByUser.contains(user)) {
      chargingBillsByUser(user) = chargingBillsByUser(user) :+ chargingBill
    } else {
      chargingBillsByUser(user) = Seq(chargingBill)
    }
    updateChargingBillsToInvoiceStore(chargingBill)
  }

  private def updateChargingBillsToInvoiceStore(chargingBill: ChargingBill): Unit = {
    chargingBillsToInvoice += chargingBill
  }

  def getUserBills(user: User): Seq[ChargingBill] = {
    chargingBillsByUser.getOrElse(user, Seq.empty)
  }
}
