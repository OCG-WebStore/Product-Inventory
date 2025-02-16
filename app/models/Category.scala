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

  def fromString(s: String): Option[Category] = s.toLowerCase match {
    case Hoodies.stringValue => Some(Hoodies)
    case TShirts.stringValue => Some(TShirts)
    case Trousers.stringValue => Some(Trousers)
    case Gloves.stringValue => Some(Gloves)
    case Other.stringValue => Some(Other)
    case _ => None
  }

  implicit val categoryColumnType: BaseColumnType[Category] =
    MappedColumnType.base[Category, String](
      _.stringValue,
      fromString(_).getOrElse(Other)
    )

  implicit val categoryFormat: Format[Category] = new Format[Category] {
    def reads(json: JsValue): JsResult[Category] = json.validate[String].flatMap { s =>
      fromString(s) match {
        case Some(category) => JsSuccess(category)
        case None => JsError(s"Unknown category: $s")
      }
    }

    def writes(category: Category): JsValue = JsString(category.stringValue)
  }
}
