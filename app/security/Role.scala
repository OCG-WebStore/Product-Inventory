package security

import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._

sealed trait Role {
  def stringValue: String
}

object Role {
  case object ADMIN extends Role {
    val stringValue = "admin"
  }

  case object DESIGNER extends Role {
    val stringValue = "designer"
  }

  case object CUSTOMER extends Role {
    val stringValue = "customer"
  }

  def fromString(s: String): Role = s.toLowerCase match {
    case ADMIN.stringValue => ADMIN
    case DESIGNER.stringValue => DESIGNER
    case CUSTOMER.stringValue => CUSTOMER
    case _ => throw new IllegalArgumentException("Invalid role")
  }

  implicit val categoryColumnType: BaseColumnType[Role] =
    MappedColumnType.base[Role, String](
      _.stringValue,
      fromString
    )

  implicit val categoryFormat: Format[Role] = new Format[Role] {
    def reads(json: JsValue): JsResult[Role] = json.validate[String].flatMap { s =>
      JsSuccess(fromString(s))
    }

    def writes(category: Role): JsValue = JsString(category.stringValue)
  }
}
