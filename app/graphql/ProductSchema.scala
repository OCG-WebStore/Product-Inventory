package graphql

import models.{Category, Product}
import services.ProductService
import sangria.schema.{fields, _}


object ProductSchema {

  import CustomScalars.Implicits._

  implicit val CategoryType: EnumType[Category] = EnumType(
    "Category",
    Some("The category of the product"),
    List(
      EnumValue("HOODIES",
        value = Category.Hoodies,
        description = Some("Hoodies category")),
      EnumValue("TSHIRTS",
        value = Category.TShirts,
        description = Some("T-Shirts category")),
      EnumValue("TROUSERS",
        value = Category.Trousers,
        description = Some("Trousers category")),
      EnumValue("GLOVES",
        value = Category.Gloves,
        description = Some("Gloves category")),
      EnumValue("OTHER",
        value = Category.Other,
        description = Some("Other products"))
    )
  )

  implicit val ProductType: ObjectType[Any, Product] = ObjectType[Any, Product](
    "Product",
    "A product in the cataloger",
    fields[Any, Product](
      Field("id", OptionType(IDType), resolve = _.value.id.map(_.toString)),
      Field("name", StringType, resolve = _.value.name),
      Field("description", StringType, resolve = _.value.description),
      Field("price", LongType, resolve = _.value.price),
      Field("category", CategoryType, resolve = _.value.category),
      Field("imageKey", StringType, resolve = _.value.imageKey),
      Field("customizable", BooleanType, resolve = _.value.customizable),
      Field("createdAt", InstantType, resolve = _.value.createdAt)
    )
  )

  val QueryType: ObjectType[ProductService, Any] = ObjectType(
    "Query",
    fields = fields[ProductService, Any](
      Field(
        "allProducts",
        ListType(ProductType),
        description = Some("Returns a list of products matching criteria."),
        resolve = c => c.ctx.getAllProducts
      ),
      Field(
        "productById",
        OptionType(ProductType),
        description = Some("Returns a product by id."),
        arguments = Argument("id", LongType) :: Nil,
        resolve = c => c.ctx.getProduct(c.arg("id"))

      ),
      Field(
        "productsByCategory",
        ListType(ProductType),
        description = Some("Returns a list of products by category."),
        arguments = Argument("category", StringType) :: Nil,
        resolve = c => c.ctx.getByCategory(c.arg("category"))
      ),
    )
  )


  val schema: Schema[ProductService, Any] = Schema(
    query = QueryType,
    additionalTypes = List(InstantType, LongType)
  )
}
