package models

import models.errors.{InsufficientInventory, InventoryError, ReservationNotFound}
import play.api.libs.json.{Format, Json}

case class Inventory(
                      productId: String,
                      stockQuantity: Int,
                      reservedQuantity: Int,
                      version: Long
                    ) {
  def availableQuantity: Int = stockQuantity - reservedQuantity
  
  def canReserve(amount: Int): Boolean = availableQuantity >= amount

  def reserve(amount: Int): Either[InventoryError, Inventory] =
    if (canReserve(amount)) Right(copy(reservedQuantity = reservedQuantity + amount))
    else Left(InsufficientInventory(productId, amount, availableQuantity))

  def confirmReservation(amount: Int): Either[InventoryError, Inventory] =
    if (reservedQuantity >= amount) Right(copy(stockQuantity = stockQuantity - amount, reservedQuantity = reservedQuantity - amount))
    else Left(ReservationNotFound(productId, amount))

  def cancelReservation(amount: Int): Either[InventoryError, Inventory] =
    if (reservedQuantity >= amount) Right(copy(reservedQuantity = reservedQuantity - amount))
    else Left(ReservationNotFound(productId, amount))

  def addStock(amount: Int): Inventory = copy(stockQuantity = stockQuantity + amount)
}

object Inventory {
  implicit val inventoryFormat: Format[Inventory] = Json.format[Inventory]

  def empty(productId: String): Inventory = Inventory(productId, 0, 0, 0)
}
