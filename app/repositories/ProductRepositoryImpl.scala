package repositories

import controllers.commands.{CreateProductCommand, UpdateProductCommand}
import models.{Category, Product}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}

import java.time.Instant
import javax.inject.{Inject, Singleton}

@Singleton
class ProductRepositoryImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends ProductRepository {

  private class Products(tag: Tag) extends Table[Product](tag, "products") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def description = column[String]("DESCRIPTION")
    def price = column[Long]("PRICE")
    def category = column[Category]("CATEGORY")
    def imageKey = column[String]("IMAGE_KEY")
    def customizable = column[Boolean]("CUSTOMIZABLE")
    def createdAt = column[Instant]("CREATED_AT")
    def updatedAt = column[Instant]("UPDATED_AT")

    def * =
      (id.?, name, description, price, category, imageKey, customizable, createdAt, updatedAt) <>
        ((Product.apply _).tupled,
          Product.unapply)
  }

  private val products = TableQuery[Products]

  private val db = dbConfigProvider.get[JdbcProfile].db

  override def create(command: CreateProductCommand): Future[Product] = {
    val query = products.returning(products.map(_.id)).into {
      case (prod, generatedId) => prod.copy(id = Some(generatedId))
    }
    db.run(query += CreateProductCommand.toProduct(command))
  }

  override def findById(id: Long): Future[Option[Product]] =
    db.run(products.filter(_.id === id).result.headOption)

  override def search(filter: ProductFilter = ProductFilter(None, None)): Future[Seq[Product]] = {
    val query = products
      .filterOpt(filter.minPrice)(_.price >= _)
      .filterOpt(filter.maxPrice)(_.price <= _)
    db.run(query.result)
  }

  override def findAll(): Future[Seq[Product]] = search()


  override def update(id: Long, command: UpdateProductCommand): Future[Option[Product]] = {
    val query = for {
      existing <- products.filter(_.id === id).result.headOption
      updated <- existing match {
        case Some(product) =>
          products
            .filter(_.id === id)
            .map(p => (
              p.name,
              p.description,
              p.price,
              p.category,
              p.imageKey,
              p.updatedAt
            ))
            .update((
              command.name.getOrElse(product.name),
              command.description.getOrElse(product.description),
              command.price.getOrElse(product.price),
              command.category.getOrElse(product.category),
              command.imageKey.getOrElse(product.imageKey),
              Instant.now()
            )).flatMap(_ => products.filter(_.id === id).result.headOption)
        case None => DBIO.successful(None)
      }
    } yield updated

    db.run(query.transactionally)
  }


  override def delete(id: Long): Future[Boolean] = {
    val query = products.filter(_.id === id).delete
    db.run(query).map(_ > 0)
  }

}

case class ProductFilter(
                        minPrice: Option[Long] = None,
                        maxPrice: Option[Long] = None
                        )
