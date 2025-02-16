package controllers.commands

import models.Category
import play.api.libs.json.{Format, Json}

case class UpdateProductCommand(
                                 name: Option[String],
                                 description: Option[String],
                                 price: Option[Long],
                                 category: Option[Category],
                                 imageKey: Option[String],
                                 customizable: Option[Boolean]
                               ) extends ProductCommand[UpdateProductCommand]

object UpdateProductCommand {
  implicit val format: Format[UpdateProductCommand] = Json.format[UpdateProductCommand]
}