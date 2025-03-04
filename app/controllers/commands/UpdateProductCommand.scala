package controllers.commands

import models.Category
import play.api.libs.json.{Format, Json}

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