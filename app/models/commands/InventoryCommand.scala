package models.commands

import com.google.inject._
import events._
import models.Inventory
import models.errors.{EventPublishingError, InventoryError, StateRetrievalError}
import play.api.libs.json.{Format, Json}

import scala.concurrent.{ExecutionContext, Future}


sealed trait InventoryCommand {
  def productId: String
}

case class AddStock(
                     productId: String,
                     quantity: Int
                   ) extends InventoryCommand

case class ReserveStock(
                         productId: String,
                         quantity: Int,
                         reservationId: String
                       ) extends InventoryCommand

case class ConfirmReservation(
                               productId: String,
                               quantity: Int,
                               reservationId: String
                             ) extends InventoryCommand

case class CancelReservation(
                               productId: String,
                               quantity: Int,
                               reservationId: String
                             ) extends InventoryCommand

object InventoryCommand {
  implicit val addStockFormat: Format[AddStock] = Json.format[AddStock]
  implicit val reserveStockFormat: Format[ReserveStock] = Json.format[ReserveStock]
  implicit val confirmReservationFormat: Format[ConfirmReservation] = Json.format[ConfirmReservation]
  implicit val cancelReservation: Format[CancelReservation] = Json.format[CancelReservation]
}

@Singleton
class InventoryCommandHandler @Inject()(eventStore: InventoryEventStore)(implicit ec: ExecutionContext) {

  //TODO:
  // 1. Events are published regardless of whether the command is processed successfully or not
  // 2. ConfirmReservation does not decrease the quantity in published states

  def handle(command: AddStock): Future[Either[InventoryError, Inventory]] = {
    for {
      currentStateOpt <- eventStore.getCurrentState(command.productId)
      currentVersion = currentStateOpt.map(_.version).getOrElse(0L)
      event = StockAdded(
        productId = command.productId,
        quantity = command.quantity,
        version = currentVersion + 1
      )
      _ <- eventStore.publish(event).recover {
        case error: EventPublishingError => throw error
        case ex => throw EventPublishingError(ex.getMessage, Some(ex))
      }
      newState <- eventStore.getCurrentState(command.productId)
    } yield newState match {
      case Some(state) => Right(state)
      case None => Right(Inventory(command.productId, command.quantity, 0, 1))
    }
  }

  def handle(command: ReserveStock): Future[Either[InventoryError, Inventory]] = {
    for {
      currentStateOpt <- eventStore.getCurrentState(command.productId)
      currentVersion = currentStateOpt.map(_.version).getOrElse(0L)
      event = StockReserved(
        productId = command.productId,
        quantity = command.quantity,
        reservationId = command.reservationId,
        version = currentVersion + 1
      )
      _ <- eventStore.publish(event)
      newState <- eventStore.getCurrentState(command.productId)
    } yield newState.toRight(StateRetrievalError("Failed to get updated state"))
  }

  def handle(command: ConfirmReservation): Future[Either[InventoryError, Inventory]] = {
    for {
      currentStateOpt <- eventStore.getCurrentState(command.productId)
      currentVersion = currentStateOpt.map(_.version).getOrElse(0L)
      event = ReservationConfirmed(
        productId = command.productId,
        quantity = command.quantity,
        reservationId = command.reservationId,
        version = currentVersion + 1
      )
      _ <- eventStore.publish(event)
      newState <- eventStore.getCurrentState(command.productId)
    } yield newState.toRight(StateRetrievalError("Failed to get updated state"))
  }

  def handle(command: CancelReservation): Future[Either[InventoryError, Inventory]] = {
    for {
      currentStateOpt <- eventStore.getCurrentState(command.productId)
      currentVersion = currentStateOpt.map(_.version).getOrElse(0L)
      event = ReservationCancelled(
        productId = command.productId,
        quantity = command.quantity,
        reservationId = command.reservationId,
        version = currentVersion + 1
      )
      _ <- eventStore.publish(event)
      newState <- eventStore.getCurrentState(command.productId)
    } yield newState.toRight(StateRetrievalError("Failed to get updated state"))
  }

  private def publish(event: InventoryEvent, command: InventoryCommand): Future[Either[StateRetrievalError, Inventory]] = {
    eventStore.publish(event)
    eventStore.getCurrentState(command.productId).map(newState => {
      newState.toRight(StateRetrievalError("Failed to get updated state"))
    })
  }
}
