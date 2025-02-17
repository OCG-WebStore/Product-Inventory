package models

import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._

sealed trait Category {
  def stringValue: String
}

object Category {
  case object Hoodies extends Category {
    val stringValue = "hoodies"
  }

  case object TShirts extends Category {
    val stringValue = "t-shirts"
  }

  case object Trousers extends Category {
    val stringValue = "trousers"
  }

  case object Gloves extends Category {
    val stringValue = "gloves"
  }

  case object Other extends Category {
    val stringValue = "other"
  }

  def fromString(s: String): Category = s.toLowerCase match {
    case Hoodies.stringValue => Hoodies
    case TShirts.stringValue => TShirts
    case Trousers.stringValue => Trousers
    case Gloves.stringValue => Gloves
    case _ => Other
  }

  implicit val categoryColumnType: BaseColumnType[Category] =
    MappedColumnType.base[Category, String](
      _.stringValue,
      fromString
    )

  implicit val categoryFormat: Format[Category] = new Format[Category] {
    def reads(json: JsValue): JsResult[Category] = json.validate[String].flatMap { s =>
      JsSuccess(fromString(s))
    }

    def writes(category: Category): JsValue = JsString(category.stringValue)
  }
}
