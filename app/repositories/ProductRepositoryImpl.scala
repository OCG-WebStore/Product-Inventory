package repositories

import models.{Category, Product}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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

    def * =
      (id.?, name, description, price, category, imageKey.?, customizable, createdAt) <>
        ((Product.apply _).tupled,
          Product.unapply)
  }

  private val products = TableQuery[Products]

  private val db = dbConfigProvider.get[JdbcProfile].db

  override def create(product: Product): Future[Product] = {
    val query = products.returning(products.map(_.id)).into {
      case (prod, generatedId) => prod.copy(id = Some(generatedId))
    }
    db.run(query += product)
  }

  override def findById(id: Long): Future[Option[Product]] =
    db.run(products.filter(_.id === id).result.headOption)

  def search(filter: ProductFilter = ProductFilter(None, None)): Future[Seq[Product]] = {
    val query = products
      .filterOpt(filter.minPrice)(_.price >= _)
      .filterOpt(filter.maxPrice)(_.price <= _)
    db.run(query.result)
  }

  override def findAll(): Future[Seq[Product]] = search()


  override def update(id: Long, product: Product): Future[Option[Product]] = {
    val query = (for {
      rowsUpdated <- products.filter(_.id === id).update(product)
      result <- if (rowsUpdated > 0) {
        products.filter(_.id === id).result.headOption
      }
      else {
        DBIO.successful(None)
      }
    } yield result).transactionally

    db.run(query)
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
