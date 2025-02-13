package controllers.request

import models.{Category, Product}
import play.api.libs.json.{Format, Json}

case class CreateProductRequest(
                                 name: String,
                                 description: String,
                                 price: Long,
                                 stockQuantity: Int,
                                 category: Category,
                                 imageUrl: Option[String],
                                 customizable: Boolean = false
                               )

object CreateProductRequest {
  implicit val format: Format[CreateProductRequest] = Json.format

  def toProduct(request: CreateProductRequest): Product = {
    Product(None, request.name, request.description, request.price, request.category, request.imageUrl, request.customizable)
  }
}