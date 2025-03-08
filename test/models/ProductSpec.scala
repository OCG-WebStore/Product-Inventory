package models

import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsSuccess, Json}

import java.time.Instant


@RunWith(classOf[JUnitRunner])
class ProductSpec extends AnyWordSpec with Matchers {

  "Product" should {
    val fixedInstant = Instant.parse("2023-01-01T00:00:00Z")

    "be instantiated with all required fields" in {
      val product = Product(
        id = Some(1L),
        name = "Test Product",
        description = "A test product",
        price = 1000L,
        category = Category.Other,
        imageKey = "test-image.jpg"
      )

      product.id shouldBe Some(1L)
      product.name shouldBe "Test Product"
      product.description shouldBe "A test product"
      product.price shouldBe 1000L
      product.category shouldBe Category.Other
      product.imageKey shouldBe "test-image.jpg"
      product.customizable shouldBe false // default value
    }

    "be instantiated with optional fields" in {
      val product = Product(
        id = Some(1L),
        name = "Test Product",
        description = "A test product",
        price = 1000L,
        category = Category.Other,
        imageKey = "test-image.jpg",
        customizable = true,
        createdAt = fixedInstant,
        updatedAt = fixedInstant
      )

      product.customizable shouldBe true
      product.createdAt shouldBe fixedInstant
      product.updatedAt shouldBe fixedInstant
    }

    "be convertible to JSON" in {
      val product = Product(
        id = Some(1L),
        name = "Test Product",
        description = "A test product",
        price = 1000L,
        category = Category.TShirts,
        imageKey = "test-image.jpg",
        customizable = true,
        createdAt = fixedInstant,
        updatedAt = fixedInstant
      )

      val json = Json.toJson(product)
      (json \ "id").as[Long] shouldBe 1L
      (json \ "name").as[String] shouldBe "Test Product"
      (json \ "description").as[String] shouldBe "A test product"
      (json \ "price").as[Long] shouldBe 1000L
      (json \ "category").as[String] shouldBe "t-shirts"
      (json \ "imageKey").as[String] shouldBe "test-image.jpg"
      (json \ "customizable").as[Boolean] shouldBe true
      (json \ "createdAt").as[String] shouldBe "2023-01-01T00:00:00Z"
      (json \ "updatedAt").as[String] shouldBe "2023-01-01T00:00:00Z"
    }

    "be parsed from JSON" in {
      val json = Json.parse(
        """
          |{
          |  "id": 1,
          |  "name": "Test Product",
          |  "description": "A test product",
          |  "price": 1000,
          |  "category": "t-shirts",
          |  "imageKey": "test-image.jpg",
          |  "customizable": true,
          |  "createdAt": "2023-01-01T00:00:00Z",
          |  "updatedAt": "2023-01-01T00:00:00Z"
          |}
          |""".stripMargin)

      val product = json.validate[Product]
      product shouldBe a[JsSuccess[_]]

      val parsedProduct = product.get
      parsedProduct.id shouldBe Some(1L)
      parsedProduct.name shouldBe "Test Product"
      parsedProduct.description shouldBe "A test product"
      parsedProduct.price shouldBe 1000L
      parsedProduct.category shouldBe Category.TShirts
      parsedProduct.imageKey shouldBe "test-image.jpg"
      parsedProduct.customizable shouldBe true
      parsedProduct.createdAt shouldBe fixedInstant
      parsedProduct.updatedAt shouldBe fixedInstant
    }

    "handle JSON without optional fields" in {
      val json = Json.parse(
        """
          |{
          |  "id": 1,
          |  "name": "Test Product",
          |  "description": "A test product",
          |  "price": 1000,
          |  "category": "t-shirts",
          |  "imageKey": "test-image.jpg"
          |}
          |""".stripMargin)

      val product = json.validate[Product]
      product shouldBe a[JsSuccess[_]]

      val parsedProduct = product.get
      parsedProduct.id shouldBe Some(1L)
      parsedProduct.name shouldBe "Test Product"
      parsedProduct.description shouldBe "A test product"
      parsedProduct.price shouldBe 1000L
      parsedProduct.category shouldBe Category.TShirts
      parsedProduct.imageKey shouldBe "test-image.jpg"
      parsedProduct.customizable shouldBe false
    }

    "handle product without id (for creation scenarios)" in {
      val product = Product(
        id = None,
        name = "New Product",
        description = "A new product to be created",
        price = 2000L,
        category = Category.Hoodies,
        imageKey = "new-product.jpg"
      )

      product.id shouldBe None

      val json = Json.toJson(product)
      (json \ "id").asOpt[Long] shouldBe None

      val parsed = Json.fromJson[Product](json).get
      parsed.id shouldBe None
    }
  }
}
