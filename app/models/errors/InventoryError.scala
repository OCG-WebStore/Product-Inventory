package models.errors

import play.api.libs.json.{Json, Writes}

sealed trait InventoryError extends Throwable

sealed trait BusinessError extends InventoryError
case class InsufficientInventory(productId: String, requested: Int, available: Int) extends BusinessError
case class ReservationNotFound(productId: String, amount: Int) extends BusinessError
case class ConcurrencyError(expected: String, actual: Long) extends BusinessError

sealed trait SystemError extends InventoryError
case class StateRetrievalError(message: String, cause: Option[Throwable] = None) extends SystemError
case class EventPublishingError(message: String, cause: Option[Throwable] = None) extends SystemError
case class CommandProcessingError(message: String, cause: Option[Throwable] = None) extends SystemError

object InventoryError {
  implicit val throwableWrites: Writes[Throwable] = (t: Throwable) => Json.obj("message" -> t.getMessage)

  implicit val insufficientInventoryWrites: Writes[InsufficientInventory] = Json.writes[InsufficientInventory]
  implicit val reservationNotFoundWrites: Writes[ReservationNotFound] = Json.writes[ReservationNotFound]
  implicit val concurrencyErrorWrites: Writes[ConcurrencyError] = Json.writes[ConcurrencyError]
  implicit val stateRetrievalErrorWrites: Writes[StateRetrievalError] = Json.writes[StateRetrievalError]
  implicit val eventPublishingErrorWrites: Writes[EventPublishingError] = Json.writes[EventPublishingError]
  implicit val commandProcessingErrorWrites: Writes[CommandProcessingError] = Json.writes[CommandProcessingError]

}