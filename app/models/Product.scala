package models

import play.api.libs.json._

import java.time.Instant
import java.time.temporal.ChronoUnit


case class Product(
                    id: Option[Long],
                    name: String,
                    description: String,
                    price: Long,
                    category: Category,
                    imageKey: String,
                    customizable: Boolean = false,
                    createdAt: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                    updatedAt: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
                  )

object Product {
  implicit val productFormat: Format[Product] =
    Json.format[Product]
}