package repositories

import com.typesafe.config.ConfigFactory
import controllers.commands.{CreateProductCommand, UpdateProductCommand}
import models.Category.Other
import models.{Category, Product}
import org.junit.runner.RunWith
import org.scalactic.Equality
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.{BasicProfile, DatabaseConfig}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import java.time.temporal.ChronoUnit
import java.util.TimeZone
import scala.concurrent.Await


@RunWith(classOf[JUnitRunner])
class DBProductRepositorySpec extends AnyWordSpec
  with BeforeAndAfterEach
  with Matchers
  with ScalaFutures
  with IntegrationPatience {

  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds))

  private val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("slick.dbs.default", ConfigFactory.load)
  private val dbConfigProvider = new DatabaseConfigProvider {
    override def get[P <: BasicProfile]: DatabaseConfig[P] = dbConfig.asInstanceOf[DatabaseConfig[P]]
  }
  private val repository = new DBProductRepository(dbConfigProvider)

  override def beforeEach(): Unit = {
    val create = sqlu"""
      CREATE TABLE "products" (
                          "ID" BIGSERIAL PRIMARY KEY,
                          "NAME" VARCHAR(255) NOT NULL,
                          "DESCRIPTION" TEXT,
                          "PRICE" BIGINT NOT NULL,
                          "CATEGORY" VARCHAR(100),
                          "IMAGE_KEY" VARCHAR(255),
                          "CUSTOMIZABLE" BOOLEAN,
                          "CREATED_AT" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          "UPDATED_AT" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
    """

    Await.result(dbConfigProvider.get.db.run(create), defaultPatience.timeout)
  }

  override def afterEach(): Unit = {
    val drop = sqlu"""
    DROP TABLE "products" CASCADE
    """
    Await.result(dbConfigProvider.get.db.run(drop), defaultPatience.timeout)
  }

  val testProduct: Product = Product(
    id = None,
    name = "Test Jacket",
    description = "A warm jacket",
    price = 300L,
    category = Other,
    imageKey = "test-jacket.jpg"
  )

  val testCreateCommand: CreateProductCommand = CreateProductCommand(
    name = testProduct.name,
    description = testProduct.description,
    price = testProduct.price,
    category = testProduct.category,
    imageKey = testProduct.imageKey
  )

  implicit val productNamePriceEquality: Equality[Product] = (a: Product, b: Any) => b match {
    case p: Product =>
        a.id                                        == p.id &&
        a.name                                      == p.name &&
        a.description                               == p.description &&
        a.price                                     == p.price &&
        a.category                                  == p.category &&
        a.imageKey                                  == p.imageKey &&
        a.customizable                              == p.customizable &&
        a.createdAt.truncatedTo(ChronoUnit.MILLIS)  == p.createdAt.truncatedTo(ChronoUnit.MILLIS) &&
        a.updatedAt.truncatedTo(ChronoUnit.MILLIS)  == p.updatedAt.truncatedTo(ChronoUnit.MILLIS)
    case _ => false
  }

  "ProductRepository" should {
    "persist a new product" in {
      val created = repository.create(testCreateCommand).futureValue
      val fetched = repository.findById(created.id.get).futureValue

      fetched shouldBe defined
    }

    "findById" should {
      "find a product by id" in {
        val created = repository.create(testCreateCommand).futureValue
        repository.findById(created.id.get).futureValue.map(product => {
          product.name should be(testProduct.name)
          product.description should be(testProduct.description)
          product.price should be(testProduct.price)
          product.category should be(testProduct.category)
          product.imageKey should be(testProduct.imageKey)
        })
      }

      "return None for non-existent product" in {
        repository.findById(999L).futureValue shouldBe None
      }
    }

    "findAll" should {
      "return all products" in {
        implicit val productNamePriceEquality: Equality[Product] = (a: Product, b: Any) => b match {
          case p: Product =>
            a.name  == p.name &&
            a.price == p.price &&
            a.id == p.id &&
            a.category == p.category &&
            a.description == p.description &&
            a.imageKey == p.imageKey &&
            a.createdAt.truncatedTo(ChronoUnit.MILLIS) == p.createdAt.truncatedTo(ChronoUnit.MILLIS) &&
            a.updatedAt.truncatedTo(ChronoUnit.MILLIS) == p.updatedAt.truncatedTo(ChronoUnit.MILLIS)
          case _ => false
        }

        val p1 = repository.create(testCreateCommand).futureValue
        val p2 = repository.create(testCreateCommand).futureValue

        repository.findAll().futureValue should contain allOf(p1, p2)
      }
    }

    "update" should {
      "modify existing product" in {
        val original = repository.create(testCreateCommand).futureValue
        val updateCmd = UpdateProductCommand(
          name = Some("Updated Jacket"),
          price = Some(34999)
        )
        val updated = repository.update(original.id.get, updateCmd).futureValue
        updated.get.name shouldBe "Updated Jacket"
        updated.get.price shouldBe 34999
      }
    }
    
    "delete" should {
      "remove existing product" in {
        val product = repository.create(testCreateCommand).futureValue
        repository.delete(product.id.get).futureValue shouldBe true
        repository.findById(product.id.get).futureValue shouldBe None
      }
    }

    "findByCategory" should {
      "filter products correctly" in {
        val gloves = testCreateCommand.copy(category = Category.Gloves)
        val hoodies = testCreateCommand.copy(category = Category.Hoodies)

        val p1 = repository.create(gloves).futureValue
        repository.create(hoodies).futureValue

        repository.findByCategory(Category.Gloves.stringValue).futureValue should contain only p1

      }
    }
  }
}
