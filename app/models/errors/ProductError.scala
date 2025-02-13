package models.errors

import play.api.libs.json.{Json, Writes}

sealed trait ProductError
case class ProductNotFound(id: Long) extends ProductError
case class InvalidProduct(reason: String) extends ProductError
case class DatabaseError(message: String) extends ProductError

object ProductError {
  implicit val productNotFoundWrites: Writes[ProductNotFound] = Json.writes[ProductNotFound]
  implicit val invalidProductWrites: Writes[InvalidProduct] = Json.writes[InvalidProduct]
  implicit val databaseErrorWrites: Writes[DatabaseError] = Json.writes[DatabaseError]
}