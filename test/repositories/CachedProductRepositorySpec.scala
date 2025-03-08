package repositories

import akka.actor.ActorSystem
import akka.stream.Materializer
import controllers.commands.{CreateProductCommand, UpdateProductCommand}
import models.Category.Other
import models.{Category, Product}
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.{contain, convertToAnyMustWrapper}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import services.RedisService

import scala.concurrent.{ExecutionContext, Future}


@RunWith(classOf[JUnitRunner])
class CachedProductRepositorySpec extends AnyWordSpec
  with ScalaFutures
  with MockitoSugar {

  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: Materializer = Materializer(system)
  implicit lazy val ec: ExecutionContext = system.dispatcher

  val productRepository: ProductRepository = mock[ProductRepository]
  val redisService: RedisService = mock[RedisService]
  val cachedProductRepository = new CachedProductRepository(productRepository, redisService)(ec)

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

  val testUpdateCommand: UpdateProductCommand = UpdateProductCommand(
    name = Some(testProduct.name + "_updated"),
    description = Some(testProduct.description + "_updated"),
    price = Some(testProduct.price + 100),
    category = Some(testProduct.category),
    imageKey = Some(testProduct.imageKey + "_updated")
  )

  "CachedProductRepository" should {
    "find product by id from cache" in {
      val productId = 123L
      val cachedProduct = testProduct.copy(id = Some(productId))

      when(redisService.getCachedProduct(eqTo(productId))) thenReturn Future.successful(Some(cachedProduct))

      val result = cachedProductRepository.findById(productId).futureValue

      result mustBe Some(cachedProduct)
    }

    "find product by id from database when not cached" in {
      val productId = 123L
      val dbProduct = testProduct.copy(id = Some(productId))

      when(redisService.getCachedProduct(eqTo(productId))) thenReturn Future.successful(None)
      when(productRepository.findById(eqTo(productId))) thenReturn Future.successful(Some(dbProduct))
      when(redisService.cacheProduct(eqTo(dbProduct))) thenReturn Future.successful(true)

      val result = cachedProductRepository.findById(productId).futureValue

      result mustBe Some(dbProduct)
    }

    "find products by category from cache" in {
      val category = "Other"
      val productIds = Seq(123L, 456L)
      val cachedProducts = Seq(
        testProduct.copy(id = Some(123L)),
        testProduct.copy(id = Some(456L))
      )

      when(redisService.getCachedCategoryIds(eqTo(category))) thenReturn Future.successful(productIds)
      when(redisService.getCachedProduct(eqTo(123L))) thenReturn Future.successful(Some(cachedProducts.head))
      when(redisService.getCachedProduct(eqTo(456L))) thenReturn Future.successful(Some(cachedProducts.last))

      val result = cachedProductRepository.findByCategory(category).futureValue

      result must contain theSameElementsAs cachedProducts
    }

    "find products by category from database when not cached" in {
      

      val category = "Other"
      val dbProducts = Seq(
        testProduct.copy(id = Some(123L)),
        testProduct.copy(id = Some(456L))
      )

      when(redisService.getCachedCategoryIds(eqTo(category))) thenReturn Future.successful(Seq.empty[Long])
      when(productRepository.findByCategory(eqTo(category))) thenReturn Future.successful(dbProducts)
      when(redisService.cacheCategory(eqTo(Category.fromString(category)), eqTo(dbProducts.map(_.id.get)))) thenReturn Future.successful(true)

      val result = cachedProductRepository.findByCategory(category).futureValue

      result must contain theSameElementsAs dbProducts
    }

    "create product and cache it" in {
      val savedProduct = testProduct.copy(id = Some(123L))

      when(productRepository.create(eqTo(testCreateCommand))) thenReturn Future.successful(savedProduct)
      when(redisService.getCachedCategoryIds(eqTo(savedProduct.category.stringValue))) thenReturn Future.successful(Seq.empty[Long])
      when(redisService.getCachedProductIds) thenReturn Future.successful(Seq.empty[Long])
      when(redisService.cacheAllProductsIds(eqTo(Seq(savedProduct.id.get)))) thenReturn Future.successful(true)
      when(redisService.cacheCategory(eqTo(savedProduct.category), eqTo(Seq(savedProduct.id.get)))) thenReturn Future.successful(true)
      when(redisService.cacheProduct(eqTo(savedProduct))) thenReturn Future.successful(true)

      val result = cachedProductRepository.create(testCreateCommand).futureValue

      result mustBe savedProduct
    }

    "update product and update cache" in {
      val productId = 123L
      val oldProduct = testProduct.copy(id = Some(productId))
      val updatedProduct = oldProduct.copy(
        name = oldProduct.name + "_updated",
        description = oldProduct.description + "_updated",
        price = oldProduct.price + 100,
        imageKey = oldProduct.imageKey + "_updated"
      )

      when(productRepository.findById(eqTo(productId))) thenReturn Future.successful(Some(oldProduct))
      when(productRepository.update(eqTo(productId), eqTo(testUpdateCommand))) thenReturn Future.successful(Some(updatedProduct))
      when(redisService.cacheProduct(eqTo(updatedProduct))) thenReturn Future.successful(true)

      val result = cachedProductRepository.update(productId, testUpdateCommand).futureValue

      result mustBe Some(updatedProduct)
    }

    "delete product and remove from cache" in {
      val productId = 123L
      val product = testProduct.copy(id = Some(productId))

      when(productRepository.findById(eqTo(productId))) thenReturn Future.successful(Some(product))
      when(redisService.cacheCategory(eqTo(product.category), eqTo(Seq()))) thenReturn Future.successful(true)
      when(redisService.getCachedCategoryIds(eqTo(product.category.stringValue))) thenReturn Future.successful(Seq(productId))
      when(redisService.getCachedProductIds) thenReturn Future.successful(Seq(productId))
      when(redisService.cacheAllProductsIds(eqTo(Seq()))) thenReturn Future.successful(true)
      when(redisService.removeProductCache(eqTo(productId))) thenReturn Future.successful(productId)
      when(productRepository.delete(eqTo(productId))) thenReturn Future.successful(true)

      val result = cachedProductRepository.delete(productId).futureValue

      result mustBe true
    }

    "find all products from cache" in {
      val productIds = Seq(123L, 456L)
      val cachedProducts = Seq(
        testProduct.copy(id = Some(123L)),
        testProduct.copy(id = Some(456L))
      )

      when(redisService.getCachedProductIds) thenReturn Future.successful(productIds)
      when(redisService.getCachedProduct(eqTo(123L))) thenReturn Future.successful(Some(cachedProducts.head))
      when(redisService.getCachedProduct(eqTo(456L))) thenReturn Future.successful(Some(cachedProducts.last))

      val result = cachedProductRepository.findAll().futureValue

      result must contain theSameElementsAs cachedProducts
    }

    "find all products from database when not cached" in {
      val dbProducts = Seq(
        testProduct.copy(id = Some(123L)),
        testProduct.copy(id = Some(456L))
      )

      when(redisService.getCachedProductIds) thenReturn Future.successful(Seq.empty[Long])
      when(productRepository.findAll()) thenReturn Future.successful(dbProducts)
      when(redisService.cacheAllProductsIds(eqTo(dbProducts.map(_.id.get)))) thenReturn Future.successful(true)
      when(redisService.cacheProduct(eqTo(dbProducts.head))) thenReturn Future.successful(true)
      when(redisService.cacheProduct(eqTo(dbProducts.last))) thenReturn Future.successful(true)

      val result = cachedProductRepository.findAll().futureValue

      result must contain theSameElementsAs dbProducts
    }

    "update product with category change" in {
      val productId = 123L
      val oldProduct = testProduct.copy(id = Some(productId))
      val updatedProduct = oldProduct.copy(category = Category.fromString("hoodies"))

      when(productRepository.findById(eqTo(productId))) thenReturn Future.successful(Some(oldProduct))
      when(productRepository.update(eqTo(productId), eqTo(testUpdateCommand.copy(category = Some(updatedProduct.category))))) thenReturn Future.successful(Some(updatedProduct))
      when(redisService.cacheProduct(eqTo(updatedProduct))) thenReturn Future.successful(true)
      when(redisService.getCachedCategoryIds(eqTo(oldProduct.category.stringValue))) thenReturn Future.successful(Seq(productId))
      when(redisService.cacheCategory(eqTo(oldProduct.category), eqTo(Seq()))) thenReturn Future.successful(true)
      when(redisService.getCachedCategoryIds(eqTo(updatedProduct.category.stringValue))) thenReturn Future.successful(Seq.empty)
      when(redisService.cacheCategory(eqTo(updatedProduct.category), eqTo(Seq(productId)))) thenReturn Future.successful(true)

      val result = cachedProductRepository.update(productId, testUpdateCommand.copy(category = Some(updatedProduct.category))).futureValue

      result mustBe Some(updatedProduct)
    }

    "update product without category change" in {
      val productId = 123L
      val oldProduct = testProduct.copy(id = Some(productId))
      val updatedProduct = oldProduct.copy(name = oldProduct.name + "_updated")

      when(productRepository.findById(eqTo(productId))) thenReturn Future.successful(Some(oldProduct))
      when(productRepository.update(eqTo(productId), eqTo(testUpdateCommand.copy(name = Some(updatedProduct.name))))) thenReturn Future.successful(Some(updatedProduct))
      when(redisService.cacheProduct(eqTo(updatedProduct))) thenReturn Future.successful(true)

      val result = cachedProductRepository.update(productId, testUpdateCommand.copy(name = Some(updatedProduct.name))).futureValue

      result mustBe Some(updatedProduct)
    }

    "delete product and update category cache when product exists in category" in {
      val productId = 123L
      val product = testProduct.copy(id = Some(productId))

      when(productRepository.findById(eqTo(productId))) thenReturn Future.successful(Some(product))
      when(redisService.getCachedCategoryIds(eqTo(product.category.stringValue))) thenReturn Future.successful(Seq(productId))
      when(redisService.cacheCategory(eqTo(product.category), eqTo(Seq()))) thenReturn Future.successful(true)
      when(redisService.getCachedProductIds) thenReturn Future.successful(Seq(productId))
      when(redisService.cacheAllProductsIds(Seq())) thenReturn Future.successful(true)
      when(redisService.removeProductCache(eqTo(productId))) thenReturn Future.successful(productId)
      when(productRepository.delete(eqTo(productId))) thenReturn Future.successful(true)

      val result = cachedProductRepository.delete(productId).futureValue

      result mustBe true
    }
  }
}