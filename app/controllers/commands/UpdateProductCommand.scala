package controllers.commands

import models.Category

case class UpdateProductCommand(
                                 name: Option[String],
                                 description: Option[String],
                                 price: Option[Long],
                                 category: Option[Category],
                                 imageKey: Option[String],
                                 customizable: Option[Boolean]
                               ) extends ProductCommand