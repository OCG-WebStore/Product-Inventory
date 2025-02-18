package repositories

import controllers.commands.{CreateProductCommand, UpdateProductCommand}

import javax.inject.{Inject, Named, Singleton}
import models.{Category, Product}
import play.api.libs.json.{JsError, JsSuccess, Json}
import services.RedisService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CachedProductRepository @Inject() (
                                          @Named("db")
                                          productRepository: ProductRepository,
                                          redis: RedisService
                                        )(implicit ec: ExecutionContext) extends ProductRepository {

  //TODO:  Define better border between CachedProductRepository and CacheService
  //TODO:  Move cache logic to CacheService and keep repository related methods here
  //TODO:  Refactoring - unload CRUD methods' verbosity by creating helper methods like def cacheProducts
  //TODO:  Current implementation works well but might be hard to read and maintain

  //TODO:  Move Keys to CacheService
  private def productKey(id: Long): String = s"product:$id"
  private def categoryKey(category: Category): String =
    s"products:category:${category.stringValue}"

  private def cacheProduct(product: Product): Future[Boolean] = {
    val key = productKey(product.id.get)
    redis.set(key, Json.toJson(product).toString)
  }

  private def cacheCategory(category: Category, ids: List[Long]): Future[Boolean] = {
    val key = categoryKey(category)
    redis.set(key, Json.toJson(ids).toString)
  }
  private def getCachedCategoryIds(category: String): Future[Seq[Long]] = {
    redis.get(categoryKey(Category.fromString(category))).map { idsStrOpt =>
      idsStrOpt
        .map(str => Json.parse(str).as[Seq[Long]])
        .getOrElse(Seq.empty[Long])
    }
  }

  private def getCachedProduct(id: Long): Future[Option[Product]] = {
    redis.get(productKey(id)).map { jsonStrOpt =>
      jsonStrOpt.flatMap { jsonStr =>
        Json.parse(jsonStr).asOpt[Product]
      }
    }
  }

  private def getProducts(ids: List[Long]): Future[List[Product]] = {
    def fallbackToDb(id: Long) = productRepository.findById(id).map(_.get)
    Future.traverse(ids) { id =>
      redis.get(productKey(id)).flatMap {
        case Some(json) =>
          Json.parse(json).validate[Product] match {
            case JsSuccess(product, _) => Future.successful(product)
            case JsError(_) => fallbackToDb(id)
          }
        case None =>
          fallbackToDb(id)
      }
    }
  }

  override def findById(id: Long): Future[Option[Product]] = {
    def fallBackToDb(id: Long) = {
      productRepository.findById(id).flatMap {
        case Some(p) => cacheProduct(p).map(_ => Some(p))
        case None => Future.successful(None)
      }
    }
    val key = productKey(id)

    redis.get(key).flatMap {
      case Some(json) =>
        Json.parse(json).validate[Product] match {
          case JsSuccess(product, _) => Future.successful(Some(product))
          case _ =>
            fallBackToDb(id)
        }

      case None =>
        fallBackToDb(id)
    }
  }

  override def findByCategory(category: String): Future[Seq[Product]] = {
    def fallbackToDb(cat: Category) = {
      productRepository.findByCategory(category).flatMap { products =>
        val ids = products.map(_.id.get).toList
        cacheCategory(cat, ids).map(_ => products)
      }
    }
    val cat: Category = Category.fromString(category)
    val key = categoryKey(cat)

    redis.get(key).flatMap {
      case Some(json) =>
        Json.parse(json).validate[List[Long]] match {
          case JsSuccess(ids, _) => getProducts(ids)
          case _ =>
            fallbackToDb(cat)
        }

      case None =>
        fallbackToDb(cat)
    }
  }

  override def create(command: CreateProductCommand): Future[Product] = {
    for {
      savedProduct <- productRepository.create(command)
      categoryIds <- getCachedCategoryIds(savedProduct.category.stringValue)
      updatedIds = categoryIds :+ savedProduct.id.get
      allIds <- redis.getAllProductIds
      _ <- redis.setAllProductIds(allIds :+ savedProduct.id.get)
      _ <- redis.set(categoryKey(savedProduct.category), Json.toJson(updatedIds).toString)
      _ <- redis.set(productKey(savedProduct.id.get), Json.toJson(savedProduct).toString)
    } yield savedProduct
  }

override def update(id: Long, command: UpdateProductCommand): Future[Option[Product]] = {
  for {
    oldProductOpt <- productRepository.findById(id)
    updatedProductOpt <- productRepository.update(id, command)
    result <- updatedProductOpt match {
      case Some(updatedProduct) =>
        for {
          _ <- oldProductOpt.map { oldProduct =>
            if (oldProduct.category != updatedProduct.category) {
              getCachedCategoryIds(oldProduct.category.stringValue).map { ids =>
                redis.set(categoryKey(oldProduct.category), Json.toJson(ids.filterNot(_ == id)).toString)
              }
              getCachedCategoryIds(updatedProduct.category.stringValue).map { ids =>
                redis.set(categoryKey(updatedProduct.category), Json.toJson(ids :+ id).toString)
              }
            } else Future.successful(())
          }.getOrElse(Future.successful(()))

          _ <- redis.set(productKey(id), Json.toJson(updatedProduct).toString)
        } yield Some(updatedProduct)
      case None =>
        Future.successful(None)
    }
  } yield result
}

  override def delete(id: Long): Future[Boolean] = {
    for {
      productOpt <- productRepository.findById(id)
      isDeleted <- productOpt match {
        case Some(product) =>
          for {
            categoryIds <- getCachedCategoryIds(product.category.stringValue)
            updatedIds = categoryIds.filterNot(_ == id)
            allIds <- redis.getAllProductIds
            _ <- redis.set(categoryKey(product.category), Json.toJson(updatedIds).toString)
            _ <- redis.setAllProductIds(allIds.filterNot(_ == id))
            _ <- redis.delete(productKey(id))
            deleted <- productRepository.delete(id)
          } yield deleted
        case None =>
          Future.successful(false)
      }
    } yield isDeleted
  }

  override def findAll(): Future[Seq[Product]] = {
    for {
      productIds <- redis.getAllProductIds
      products <- if (productIds.nonEmpty) {
        Future.sequence(productIds.map(getCachedProduct))
          .map(_.flatten)
      } else {
        for {
          products <- productRepository.findAll()
          _ <- Future.sequence(products.map { product =>
            redis.set(productKey(product.id.get), Json.toJson(product).toString)
          })
          _ <- redis.setAllProductIds(products.map(_.id.get))
          _ <- Future.sequence(products.groupBy(_.category).map { case (category, categoryProducts) =>
            redis.setList(categoryKey(category), categoryProducts.map(_.id.get))
          })
        } yield products
      }
    } yield products
  }
}