package events

import models.Inventory
import play.api.Logging
import play.api.libs.json.{Format, Json}

import java.time.Instant

sealed trait InventoryEvent {
  def productId: String
  def occurredAt: Instant
  def version: Long
  def stringValue : String
}

case class StockAdded(
                       productId: String,
                       quantity: Int,
                       occurredAt: Instant = Instant.now(),
                       version: Long
                     ) extends InventoryEvent {
  override def stringValue: String = "StockAdded"
}

case class StockReserved(
                          productId: String,
                          quantity: Int,
                          reservationId: String,
                          occurredAt: Instant = Instant.now(),
                          version: Long
                        ) extends InventoryEvent {
  override def stringValue: String = "StockReserved"
}

case class ReservationConfirmed(
                                 productId: String,
                                 quantity: Int,
                                 reservationId: String,
                                 occurredAt: Instant = Instant.now(),
                                 version: Long
                               ) extends InventoryEvent {
  override def stringValue: String = "ReservationConfirmed"
}

case class ReservationCancelled(
                                 productId: String,
                                 quantity: Int,
                                 reservationId: String,
                                 occurredAt: Instant = Instant.now(),
                                 version: Long
                               ) extends InventoryEvent {
  override def stringValue: String = "ReservationCancelled"
}

object InventoryEvent {
  implicit val stockAddedFormat: Format[StockAdded] = Json.format[StockAdded]
  implicit val stockReservedFormat: Format[StockReserved] = Json.format[StockReserved]
  implicit val reservationConfirmedFormat: Format[ReservationConfirmed] = Json.format[ReservationConfirmed]
  implicit val reservationCancelledFormat: Format[ReservationCancelled] = Json.format[ReservationCancelled]
}

object InventoryEvents extends Logging {
  def apply(maybeInventory: Option[Inventory], event: InventoryEvent): Inventory = {
    val inventory = maybeInventory.getOrElse(Inventory.empty(event.productId))

    logger.debug(s"Applying event ${event.stringValue} to inventory $inventory")

    event match {
      case e: StockAdded =>
        inventory.addStock(e.quantity).copy(version = e.version)

      case e: StockReserved =>
        inventory.reserve(e.quantity).getOrElse(inventory).copy(version = e.version)

      case e: ReservationConfirmed =>
        inventory.confirmReservation(e.quantity).getOrElse(inventory).copy(version = e.version)

      case e: ReservationCancelled =>
        inventory.cancelReservation(e.quantity).getOrElse(inventory).copy(version = e.version)
    }
  }

  def rebuild(receivedEvents: Seq[InventoryEvent]): Option[Inventory] = {
    receivedEvents match {
      case Nil => None
      case events =>
        val productId = events.head.productId
        Some(events.foldLeft(Inventory.empty(productId))((item, event) => apply(Some(item), event)))
    }
  }

}