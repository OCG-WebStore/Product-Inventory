package controllers.commands

import models.{Category, Product}
import play.api.libs.json.{Format, Json}

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