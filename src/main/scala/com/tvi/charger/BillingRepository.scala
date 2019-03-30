package com.tvi.charger

import com.tvi.charger.models.{ChargingBill, User}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class BillingRepository {
  private val chargingBillsByUser = mutable.Map[User, Seq[ChargingBill]]()
  private val chargingBillsToInvoice = ListBuffer[ChargingBill]()

  def updateUserChargingBillStore(user: User, chargingBill: ChargingBill): Unit = {
    if (chargingBillsByUser.contains(user)) {
      chargingBillsByUser(user) = chargingBillsByUser(user) :+ chargingBill
    } else {
      chargingBillsByUser(user) = Seq(chargingBill)
    }
  }

  def updateChargingBillsToInvoiceStore(chargingBill: ChargingBill): Unit = {
    chargingBillsToInvoice += chargingBill
  }

  def getUserBills(user: User): Seq[ChargingBill] = {
    chargingBillsByUser.getOrElse(user, Seq.empty)
  }
}
