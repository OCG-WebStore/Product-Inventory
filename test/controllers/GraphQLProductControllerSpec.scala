package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import models.{Category, Product}
import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, ControllerComponents, Request, Result}
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}
import security.{Role, SecureActions, UserContext, UserRequest}
import services.ProductService
import org.mockito.Mockito.when
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar


@RunWith(classOf[JUnitRunner])
class GraphQLProductControllerSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar {

  implicit val system: ActorSystem = ActorSystem("TestSystem")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = Materializer(system)
  implicit val cc: ControllerComponents = Helpers.stubControllerComponents()

  val mockProductService: ProductService = mock[ProductService]
  val mockSecureActions: SecureActions = mock[SecureActions]

  val passThroughActionBuilder: ActionBuilder[UserRequest, AnyContent] =
    new ActionBuilder[UserRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = cc.parsers.anyContent
      override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] =
        block(UserRequest(UserContext(userId = Some(1L), roles = Seq(Role.ADMIN), expiresAt = 8000L), request))
      override protected def executionContext: ExecutionContext = ec
    }

  when(mockSecureActions.customerAuth).thenReturn(passThroughActionBuilder)

  val controller = new GraphQLProductController(cc, mockProductService, mockSecureActions)

  val testProduct: Product = Product(
    id = Some(1L),
    name = "Test Product",
    description = "A test product",
    price = 100L,
    category = Category.Other,
    imageKey = "test.jpg",
    customizable = false,
    createdAt = java.time.Instant.parse("2020-01-01T00:00:00Z")
  )

  "GraphQLProductController" should {

    "return all products for the allProducts query" in {
      // Stub getAllProducts to return one dummy product.
      when(mockProductService.getAllProducts).thenReturn(Future.successful(Seq(testProduct)))

      // Prepare a query that requests fields from allProducts.
      val query =
        """
          |{
          |  allProducts {
          |    id
          |    name
          |    description
          |    price
          |    category
          |    imageKey
          |    customizable
          |    createdAt
          |  }
          |}
          |""".stripMargin

      val request = FakeRequest[JsValue](
        POST,
        "/graphql/products",
        FakeHeaders(Seq(CONTENT_TYPE -> JSON)),
        Json.obj("query" -> query)
      )

      val result: Future[Result] = controller.products().apply(request)

      status(result) shouldBe OK

      val data = (contentAsJson(result) \ "data" \ "allProducts").as[Seq[JsValue]]
      data.length shouldBe 1
      (data.head \ "name").as[String] shouldBe testProduct.name
    }

    "return a product for productById query when id exists" in {
      when(mockProductService.getProduct(1L)).thenReturn(Future.successful(Some(testProduct)))

      val query =
        """query getProductById($id: Long!) {
          |  productById(id: $id) {
          |    id
          |    name
          |    description
          |  }
          |}""".stripMargin

      val variables = Json.obj("id" -> "1")
      val request = FakeRequest[JsValue](
        POST,
        "/graphql/products",
        FakeHeaders(Seq(CONTENT_TYPE -> JSON)),
        Json.obj(
          "query" -> query,
          "variables" -> variables,
          "operationName" -> "getProductById"
        )
      )

      val result = controller.products().apply(request)
      status(result) shouldBe OK

      val productJson = (contentAsJson(result) \ "data" \ "productById").as[JsValue]
      (productJson \ "name").as[String] shouldBe testProduct.name
    }

    "return null for productById query when id does not exist" in {
      when(mockProductService.getProduct(999L)).thenReturn(Future.successful(None))

      val query =
        """query getProductById($id: Long!) {
          |  productById(id: $id) {
          |    id
          |    name
          |  }
          |}""".stripMargin

      val variables = Json.obj("id" -> "999")
      val request = FakeRequest[JsValue](
        POST,
        "/graphql/products",
        FakeHeaders(Seq(CONTENT_TYPE -> JSON)),
        Json.obj(
          "query" -> query,
          "variables" -> variables,
          "operationName" -> "getProductById"
        )
      )

      val result = controller.products().apply(request)
      status(result) shouldBe OK

      (contentAsJson(result) \ "data" \ "productById").as[JsValue] shouldBe JsNull
    }

    "return products filtered by category for productsByCategory query" in {
      when(mockProductService.getByCategory("Other")).thenReturn(Future.successful(Seq(testProduct)))

      val query =
        """
          |{
          |  productsByCategory(category: "Other") {
          |    id
          |    name
          |    category
          |  }
          |}
          |""".stripMargin

      val request = FakeRequest[JsValue](
        POST,
        "/graphql/products",
        FakeHeaders(Seq(CONTENT_TYPE -> JSON)),
        Json.obj("query" -> query)
      )
      val result = controller.products().apply(request)
      status(result) shouldBe OK

      val products = (contentAsJson(result) \ "data" \ "productsByCategory").as[Seq[JsValue]]
      products should have size 1
      (products.head \ "name").as[String] shouldBe testProduct.name
    }

    "return BadRequest for an invalid GraphQL query" in {
      val invalidQuery = "query = { invalidField }"
      val request = FakeRequest[JsValue](
        POST,
        "/graphql/products",
        FakeHeaders(Seq(CONTENT_TYPE -> JSON)),
        Json.obj("query" -> invalidQuery)
      )

      val result = controller.products().apply(request)
      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "error").as[String] should include ("Syntax")
    }
  }
}