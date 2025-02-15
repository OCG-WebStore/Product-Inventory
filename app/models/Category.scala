package models

import slick.jdbc.PostgresProfile.api._

trait Category {
  def stringValue: String
}

object Category {
  val HOODIES = "hoodies"
  val TSHIRTS = "t-shirts"
  val TROUSERS = "trousers"
  val GLOVES = "gloves"

  implicit val categoryColumnType: BaseColumnType[Category] =
    MappedColumnType.base[Category,String](
      cat => cat.stringValue,
      str => Category(str)
    )

  def apply(str: String): Category = str match {
    case HOODIES => Hoodies()
    case TSHIRTS => TShirts()
    case TROUSERS => Trousers()
    case GLOVES => Gloves()
    case _ => throw new IllegalArgumentException("Invalid category")
  }

  import play.api.libs.json.{Format, Json}
  implicit val categoryFormat: Format[Category] = Json.format[Category]
}

case class Hoodies() extends Category {
  override def stringValue: String = Category.HOODIES
}

case class TShirts() extends Category {
  override def stringValue: String = Category.TSHIRTS
}

case class Trousers() extends Category {
  override def stringValue: String = Category.TROUSERS
}

case class Gloves() extends Category {
  override def stringValue: String = Category.GLOVES
}
