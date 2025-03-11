package models.commands

import models.{Category, Product}
import play.api.libs.json.{Format, Json}

sealed trait ProductCommand[T]

case class CreateProductCommand (
                                  name: String,
                                  description: String,
                                  price: Long,
                                  category: Category,
                                  imageKey: String,
                                  customizable: Boolean = false
                                ) extends ProductCommand[CreateProductCommand]

object CreateProductCommand {
  implicit val format: Format[CreateProductCommand] = Json.format[CreateProductCommand]

  def toProduct(command: CreateProductCommand): Product = {
    Product(None, command.name, command.description, command.price, command.category, command.imageKey, command.customizable)
  }
}

case class UpdateProductCommand(
                                 name: Option[String] = None,
                                 description: Option[String] = None,
                                 price: Option[Long] = None,
                                 category: Option[Category] = None,
                                 imageKey: Option[String] = None,
                                 customizable: Option[Boolean] = None
                               ) extends ProductCommand[UpdateProductCommand]

object UpdateProductCommand {
  implicit val format: Format[UpdateProductCommand] = Json.format[UpdateProductCommand]
}

