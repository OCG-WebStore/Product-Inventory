package models

import slick.jdbc.PostgresProfile.api._

case class Category(productType: String)

object Category {
  val HOODIES = "hoodies"
  val TSHIRTS = "t-shirts"
  val TROUSERS = "trousers"
  val GLOVES = "gloves"

  implicit val categoryColumnType: BaseColumnType[Category] =
    MappedColumnType.base[Category,String](
      cat => cat.productType,
      str => Category(str)
    )

  import play.api.libs.json.{Format, Json}
  implicit val categoryFormat: Format[Category] = Json.format[Category]
}
