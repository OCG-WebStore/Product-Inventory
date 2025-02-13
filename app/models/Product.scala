package models

import play.api.libs.json._

import java.time.Instant


case class Product(
                    id: Option[Long],
                    name: String,
                    description: String,
                    price: Long,
                    category: Category,
                    imageKey: Option[String] = None,
                    customizable: Boolean = false,
                    createdAt: Instant = Instant.now()
                  )

object Product {
  implicit val productFormat: Format[Product] =
    Json.format[Product]
}