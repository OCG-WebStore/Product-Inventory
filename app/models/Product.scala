package models

import play.api.libs.json._

import java.time.Instant


case class Product(
                    id: Option[Long],
                    name: String,
                    description: String,
                    price: Long,
                    category: Category,
                    imageKey: String,
                    customizable: Boolean = false,
                    createdAt: Instant = Instant.now(),
                    updatedAt: Instant = Instant.now()
                  )

object Product {

  implicit val instantFormat: Format[Instant] = new Format[Instant] {
    def reads(json: JsValue): JsResult[Instant] = json.validate[String].map(Instant.parse)
    def writes(i: Instant): JsValue = JsString(i.toString)
  }

  implicit val productFormat: Format[Product] = {
    import play.api.libs.functional.syntax._

    val reads: Reads[Product] = (
        (__ \ "id").readNullable[Long] and
        (__ \ "name").read[String] and
        (__ \ "description").read[String] and
        (__ \ "price").read[Long] and
        (__ \ "category").read[Category] and
        (__ \ "imageKey").read[String] and
        (__ \ "customizable").readWithDefault[Boolean](false) and
        (__ \ "createdAt").readWithDefault[Instant](Instant.now()) and
        (__ \ "updatedAt").readWithDefault[Instant](Instant.now())
      )(Product.apply _)

    val writes: OWrites[Product] = Json.writes[Product]

    Format(reads, writes)
  }
}