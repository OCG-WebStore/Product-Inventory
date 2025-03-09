package services

import controllers.commands.{CreateProductCommand, UpdateProductCommand}
import models.{Category, Product}
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.{any, anyLong}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import repositories.ProductRepository

import scala.concurrent.{ExecutionContext, Future}


@RunWith(classOf[JUnitRunner])
class ProductServiceImplSpec extends AnyWordSpec
  with MockitoSugar
  with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val testProduct: Product = Product(
    id = None,
    name = "Test Jacket",
    description = "A warm jacket",
    price = 300L,
    category = Category.Other,
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

  // Product with ID for repository responses
  val savedProduct: Product = testProduct.copy(id = Some(1L))
  val updatedProduct: Product = Product(
    id = Some(1L),
    name = testProduct.name + "_updated",
    description = testProduct.description + "_updated",
    price = testProduct.price + 100,
    category = testProduct.category,
    imageKey = testProduct.imageKey + "_updated"
  )

  "ProductServiceImpl" should {

    "delegate createProduct to repository" in {
      // Arrange
      val mockRepository = mock[ProductRepository]
      val productService = new ProductServiceImpl(mockRepository)

      when(mockRepository.create(any[CreateProductCommand])).thenReturn(Future.successful(savedProduct))

      // Act
      val result = productService.createProduct(testCreateCommand).futureValue

      // Assert
      verify(mockRepository, times(1)).create(testCreateCommand)
      result mustBe savedProduct
    }

    "delegate getProduct to repository" in {
      // Arrange
      val mockRepository = mock[ProductRepository]
      val productService = new ProductServiceImpl(mockRepository)

      when(mockRepository.findById(anyLong())).thenReturn(Future.successful(Some(savedProduct)))

      // Act
      val result = productService.getProduct(1L).futureValue

      // Assert
      verify(mockRepository, times(1)).findById(1L)
      result mustBe Some(savedProduct)
    }

    "delegate getByCategory to repository" in {
      // Arrange
      val mockRepository = mock[ProductRepository]
      val productService = new ProductServiceImpl(mockRepository)
      val products = Seq(
        savedProduct,
        savedProduct.copy(id = Some(2L), name = "Another Jacket")
      )

      when(mockRepository.findByCategory(any[String])).thenReturn(Future.successful(products))

      // Act
      val result = productService.getByCategory(Category.Other.stringValue).futureValue

      // Assert
      verify(mockRepository, times(1)).findByCategory(Category.Other.stringValue)
      result mustBe products
    }

    "delegate getAllProducts to repository" in {
      // Arrange
      val mockRepository = mock[ProductRepository]
      val productService = new ProductServiceImpl(mockRepository)
      val products = Seq(
        savedProduct,
        savedProduct.copy(id = Some(2L), name = "Gloves", category = Category.Gloves)
      )

      when(mockRepository.findAll()).thenReturn(Future.successful(products))

      // Act
      val result = productService.getAllProducts.futureValue

      // Assert
      verify(mockRepository, times(1)).findAll()
      result mustBe products
    }

    "delegate updateProduct to repository" in {
      // Arrange
      val mockRepository = mock[ProductRepository]
      val productService = new ProductServiceImpl(mockRepository)

      when(mockRepository.update(anyLong(), any[UpdateProductCommand])).thenReturn(Future.successful(Some(updatedProduct)))

      // Act
      val result = productService.updateProduct(1L, testUpdateCommand).futureValue

      // Assert
      verify(mockRepository, times(1)).update(1L, testUpdateCommand)
      result mustBe Some(updatedProduct)
    }

    "delegate deleteProduct to repository" in {
      // Arrange
      val mockRepository = mock[ProductRepository]
      val productService = new ProductServiceImpl(mockRepository)

      when(mockRepository.delete(anyLong())).thenReturn(Future.successful(true))

      // Act
      val result = productService.deleteProduct(1L).futureValue

      // Assert
      verify(mockRepository, times(1)).delete(1L)
      result mustBe true
    }

    "handle repository returning None for non-existent product" in {
      // Arrange
      val mockRepository = mock[ProductRepository]
      val productService = new ProductServiceImpl(mockRepository)

      when(mockRepository.findById(anyLong())).thenReturn(Future.successful(None))

      // Act
      val result = productService.getProduct(999L).futureValue

      // Assert
      verify(mockRepository, times(1)).findById(999L)
      result mustBe None
    }

    "handle repository returning None for failed update" in {
      val mockRepository = mock[ProductRepository]
      val productService = new ProductServiceImpl(mockRepository)

      when(mockRepository.update(anyLong(), any[UpdateProductCommand])).thenReturn(Future.successful(None))

      val result = productService.updateProduct(999L, testUpdateCommand).futureValue

      verify(mockRepository, times(1)).update(999L, testUpdateCommand)
      result mustBe None
    }
  }
}
