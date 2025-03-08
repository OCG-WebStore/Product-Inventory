package security

import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsString, Json}


@RunWith(classOf[JUnitRunner])
class RoleSpec extends AnyWordSpec with Matchers {

  "Role" should {
    "provide string values for all case objects" in {
      Role.ADMIN.stringValue shouldBe "admin"
      Role.DESIGNER.stringValue shouldBe "designer"
      Role.CUSTOMER.stringValue shouldBe "customer"
    }

    "create Role from string for all known roles" in {
      Role.fromString("admin") shouldBe Role.ADMIN
      Role.fromString("designer") shouldBe Role.DESIGNER
      Role.fromString("customer") shouldBe Role.CUSTOMER
    }

    "be case-insensitive when creating from string" in {
      Role.fromString("ADMIN") shouldBe Role.ADMIN
      Role.fromString("Designer") shouldBe Role.DESIGNER
      Role.fromString("cusTOMer") shouldBe Role.CUSTOMER
    }

    "throw IllegalArgumentException for unknown role strings" in {
      val exception = intercept[IllegalArgumentException] {
        Role.fromString("unknown")
      }
      exception.getMessage shouldBe "Invalid role"
    }

    "convert to JSON" in {
      Json.toJson[Role](Role.ADMIN) shouldBe JsString("admin")
      Json.toJson[Role](Role.DESIGNER) shouldBe JsString("designer")
      Json.toJson[Role](Role.CUSTOMER) shouldBe JsString("customer")
    }

    "parse from JSON" in {
      Json.fromJson[Role](JsString("admin")).get shouldBe Role.ADMIN
      Json.fromJson[Role](JsString("designer")).get shouldBe Role.DESIGNER
      Json.fromJson[Role](JsString("customer")).get shouldBe Role.CUSTOMER
    }

    "throw exception when parsing invalid JSON role" in {
      the [IllegalArgumentException]
        .thrownBy(Json.fromJson[Role](JsString("unknown"))) should have message "Invalid role"
    }

    "have a valid Slick column type mapping" in {
      val columnType = Role.categoryColumnType

      columnType.valueToSQLLiteral(Role.ADMIN) shouldBe "'admin'"
      columnType.valueToSQLLiteral(Role.DESIGNER) shouldBe "'designer'"
      columnType.valueToSQLLiteral(Role.CUSTOMER) shouldBe "'customer'"
    }
  }
}
