package models

import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsString, Json}


@RunWith(classOf[JUnitRunner])
class CategorySpec extends AnyWordSpec with Matchers {

  "Category" should {
    "provide string values for all cases" in {
      Category.Hoodies.stringValue shouldBe "hoodies"
      Category.TShirts.stringValue shouldBe "t-shirts"
      Category.Trousers.stringValue shouldBe "trousers"
      Category.Gloves.stringValue shouldBe "gloves"
      Category.Other.stringValue shouldBe "other"
    }

    "create Category from string for all known categories" in {
      Category.fromString("hoodies") shouldBe Category.Hoodies
      Category.fromString("t-shirts") shouldBe Category.TShirts
      Category.fromString("trousers") shouldBe Category.Trousers
      Category.fromString("gloves") shouldBe Category.Gloves
      Category.fromString("other") shouldBe Category.Other
    }

    "create Category.Other for unknown category strings" in {
      Category.fromString("unknown") shouldBe Category.Other
      Category.fromString("") shouldBe Category.Other
      Category.fromString("INVALID") shouldBe Category.Other
      Category.fromString("shoes") shouldBe Category.Other
    }

    "be case-insensitive when creating from string" in {
      Category.fromString("HOODIES") shouldBe Category.Hoodies
      Category.fromString("T-ShIrTs") shouldBe Category.TShirts
      Category.fromString("trousers") shouldBe Category.Trousers
      Category.fromString("GLOVES") shouldBe Category.Gloves
      Category.fromString("OtHeR") shouldBe Category.Other
    }

    "convert to JSON" in {
      Json.toJson[Category](Category.Hoodies) shouldBe JsString("hoodies")
      Json.toJson[Category](Category.TShirts) shouldBe JsString("t-shirts")
      Json.toJson[Category](Category.Trousers) shouldBe JsString("trousers")
      Json.toJson[Category](Category.Gloves) shouldBe JsString("gloves")
      Json.toJson[Category](Category.Other) shouldBe JsString("other")
    }

    "parse from JSON" in {
      Json.fromJson[Category](JsString("hoodies")).get shouldBe Category.Hoodies
      Json.fromJson[Category](JsString("t-shirts")).get shouldBe Category.TShirts
      Json.fromJson[Category](JsString("trousers")).get shouldBe Category.Trousers
      Json.fromJson[Category](JsString("gloves")).get shouldBe Category.Gloves
      Json.fromJson[Category](JsString("other")).get shouldBe Category.Other
      Json.fromJson[Category](JsString("unknown")).get shouldBe Category.Other
    }

    "have a valid Slick column type mapping" in {
      val columnType = Category.categoryColumnType

      // Map from Category to String
      columnType.valueToSQLLiteral(Category.Hoodies) shouldBe "'hoodies'"
      columnType.valueToSQLLiteral(Category.TShirts) shouldBe "'t-shirts'"
      columnType.valueToSQLLiteral(Category.Trousers) shouldBe "'trousers'"
      columnType.valueToSQLLiteral(Category.Gloves) shouldBe "'gloves'"
      columnType.valueToSQLLiteral(Category.Other) shouldBe "'other'"
    }
  }
}
