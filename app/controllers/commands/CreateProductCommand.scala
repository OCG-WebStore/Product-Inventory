package controllers.commands

import models.{Category, Product}
import play.api.libs.json.{Format, Json}

case class CreateProductCommand (
                                  name: String,
                                  description: String,
                                  price: Long,
                                  category: Category,
                                  imageKey: Option[String],
                                  customizable: Boolean = false
                                ) extends ProductCommand

object CreateProductCommand {
  implicit val format: Format[CreateProductCommand] = Json.format

  def toProduct(command: CreateProductCommand): Product = {
    Product(None, command.name, command.description, command.price, command.category, command.imageKey, command.customizable)
  }
}