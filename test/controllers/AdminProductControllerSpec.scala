package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import controllers.commands.{CreateProductCommand, UpdateProductCommand}
import models.Category.Other
import models.Product
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, StubControllerComponentsFactory}
import security.{Role, SecureActions, UserContext, UserRequest}
import services.ProductService

import scala.concurrent.{ExecutionContext, Future}


@RunWith(classOf[JUnitRunner])
class AdminProductControllerSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with StubControllerComponentsFactory  {

  implicit val cc: ControllerComponents = stubControllerComponents()
  implicit val system: ActorSystem = ActorSystem("TestSystem")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = Materializer(system)

  val parsers: PlayBodyParsers = cc.parsers

  val mockProductService: ProductService = mock[ProductService]
  val mockSecureActions: SecureActions = mock[SecureActions]

  val controller = new AdminProductController(
    cc,
    mockProductService,
    mockSecureActions
  )

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

  val passThroughActionBuilder: ActionBuilder[UserRequest, AnyContent] =
    new ActionBuilder[UserRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = parsers.anyContent

      override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
        val userRequest = UserRequest(
          UserContext(userId = Option(1L), roles = Seq(Role.ADMIN), expiresAt = 8000L),
          request
        )
        block(userRequest)
      }
      override protected def executionContext: ExecutionContext = ec
    }

  "AdminProductController" should {

    "list all products" in {
        when(mockSecureActions.adminAuth) thenReturn passThroughActionBuilder
        when(mockProductService.getAllProducts).thenReturn(Future.successful(Seq.empty))

        val result: Future[Result] = controller.list().apply(FakeRequest(GET, "/admin/products"))

        status(result) shouldBe OK
        verify(mockProductService).getAllProducts
    }

    "get a product by id" in {
        val productId = 1L
        val product = testProduct.copy(id = Some(productId))

        when(mockSecureActions.adminAuth) thenReturn passThroughActionBuilder
        when(mockProductService.getProduct(productId)).thenReturn(Future.successful(Some(product)))

        val result: Future[Result] = controller.get(productId).apply(FakeRequest(GET, s"/admin/products/$productId"))

        status(result) shouldBe OK
        verify(mockProductService).getProduct(productId)
    }

    "create a new product" in {
        val createCommand = testCreateCommand
        val productId = 1L
        val createdProduct = testProduct.copy(id = Some(productId))

        when(mockSecureActions.adminAuth) thenReturn passThroughActionBuilder
        when(mockProductService
          .createProduct(any[CreateProductCommand]))
          .thenReturn(Future.successful(createdProduct))

        val request = FakeRequest[CreateProductCommand](
          POST,
          "/admin/products",
          FakeHeaders(Seq(CONTENT_TYPE -> JSON)),
          createCommand
        )

        val result = controller.create().apply(request)

        status(result) shouldBe CREATED
        verify(mockProductService).createProduct(createCommand)
    }

    "update an existing product" in {
        val productId = 1L
        val updateCommand = testUpdateCommand
        val updatedProduct = Product(
          Some(productId),
          updateCommand.name.get,
          updateCommand.description.get,
          updateCommand.price.get,
          updateCommand.category.get,
          updateCommand.imageKey.get)

        when(mockSecureActions.adminAuth) thenReturn passThroughActionBuilder
        when(mockProductService
          .updateProduct(productId, updateCommand))
          .thenReturn(Future.successful(Some(updatedProduct)))

        val request = FakeRequest[UpdateProductCommand](
          PUT,
          s"/admin/products/$productId",
          FakeHeaders(Seq(CONTENT_TYPE -> JSON)),
          updateCommand
        )

        val result = controller.update(productId).apply(request)

        status(result) shouldBe OK
        verify(mockProductService).updateProduct(productId, updateCommand)
    }

    "delete an existing product" in {
        val productId = 1L

        when(mockSecureActions.adminAuth) thenReturn passThroughActionBuilder

        when(mockProductService.deleteProduct(productId)).thenReturn(Future.successful(true))

        val result: Future[Result] = controller
          .delete(productId)
          .apply(FakeRequest(DELETE, s"/admin/products/$productId"))

        status(result) shouldBe NO_CONTENT
        verify(mockProductService).deleteProduct(productId)
    }

    "get a product by id when product is not found" in {
      val productId = 999L
      when(mockSecureActions.adminAuth) thenReturn passThroughActionBuilder
      when(mockProductService.getProduct(eqTo(productId))).thenReturn(Future.successful(None))

      val result: Future[Result] = controller.get(productId).apply(FakeRequest(GET, s"/admin/products/$productId"))
      status(result) shouldBe NOT_FOUND
    }

    "create a new product returns BadRequest on exception" in {
      when(mockSecureActions.adminAuth) thenReturn passThroughActionBuilder
      when(mockProductService.createProduct(any[CreateProductCommand])).thenReturn(Future.failed(new RuntimeException("fail")))

      val request = FakeRequest[CreateProductCommand](
        POST,
        "/admin/products",
        FakeHeaders(Seq(CONTENT_TYPE -> JSON)),
        testCreateCommand
      )

      val result = controller.create().apply(request)
      status(result) shouldBe BAD_REQUEST
    }

    "update returns NotFound when product is not found" in {
      val productId = 2L
      when(mockSecureActions.adminAuth) thenReturn passThroughActionBuilder
      when(mockProductService.updateProduct(eqTo(productId), any[UpdateProductCommand])).thenReturn(Future.successful(None))

      val request = FakeRequest[UpdateProductCommand](
        PUT,
        s"/admin/products/$productId",
        FakeHeaders(Seq(CONTENT_TYPE -> JSON)),
        testUpdateCommand
      )

      val result = controller.update(productId).apply(request)
      status(result) shouldBe NOT_FOUND
      contentAsString(result) should include (s"Product with id $productId not found")
    }

    "update returns BadRequest when exception occurs" in {
      val productId = 3L
      when(mockSecureActions.adminAuth) thenReturn passThroughActionBuilder
      when(mockProductService.updateProduct(eqTo(productId), any[UpdateProductCommand])).thenReturn(Future.failed(new RuntimeException("error")))

      val request = FakeRequest[UpdateProductCommand](
        PUT,
        s"/admin/products/$productId",
        FakeHeaders(Seq(CONTENT_TYPE -> JSON)),
        testUpdateCommand
      )

      val result = controller.update(productId).apply(request)
      status(result) shouldBe BAD_REQUEST
    }

    "delete returns NotFound when deletion fails" in {
      val productId = 4L
      when(mockSecureActions.adminAuth) thenReturn passThroughActionBuilder
      when(mockProductService.deleteProduct(eqTo(productId))).thenReturn(Future.successful(false))

      val result = controller.delete(productId).apply(FakeRequest(DELETE, s"/admin/products/$productId"))
      status(result) shouldBe NOT_FOUND
    }

    "ping should return OK" in {
      val result: Future[Result] = controller.ping().apply(FakeRequest(GET, "/ping"))

      status(result) shouldBe OK
    }
  }
}