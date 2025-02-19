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

  private def getProducts(ids: Seq[Long]): Future[Seq[Product]] = {
    def fallbackToDb(id: Long): Future[Product] = productRepository.findById(id).map(_.get)
    Future.traverse(ids) { id =>
      redis.getCachedProduct(id).flatMap {
        case Some(product) => Future.successful(product)
        case None => fallbackToDb(id)
      }
    }
  }

  override def findById(id: Long): Future[Option[Product]] = {
    def fallBackToDb(id: Long) = {
      productRepository.findById(id).flatMap {
        case Some(p) => redis.cacheProduct(p).map(_ => Some(p))
        case None => Future.successful(None)
      }
    }

    redis.getCachedProduct(id).flatMap {
      case Some(product) => Future.successful(Some(product))
      case None =>
        fallBackToDb(id)
    }
  }

  override def findByCategory(category: String): Future[Seq[Product]] = {
    def fallbackToDb(cat: Category): Future[Seq[Product]] = {
      productRepository.findByCategory(category).flatMap { products =>
        val ids = products.map(_.id.get).toList
        redis.cacheCategory(cat, ids).map(_ => products)
      }
    }

    redis.getCachedCategoryIds(category).flatMap {
      case Nil =>
        fallbackToDb(Category.fromString(category))
      case ids => getProducts(ids)
    }
  }

  override def create(command: CreateProductCommand): Future[Product] = {
    for {
      savedProduct <- productRepository.create(command)
      categoryIds <- redis.getCachedCategoryIds(savedProduct.category.stringValue)
      updatedIds = categoryIds :+ savedProduct.id.get
      allIds <- redis.getCachedProductIds
      _ <- redis.cacheAllProductsIds(allIds :+ savedProduct.id.get)
      _ <- redis.cacheCategory(savedProduct.category, updatedIds)
      _ <- redis.cacheProduct(savedProduct)
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
              redis.getCachedCategoryIds(oldProduct.category.stringValue).map { ids =>
                redis.cacheCategory(oldProduct.category, ids.filterNot(_ == id))
              }
              redis.getCachedCategoryIds(updatedProduct.category.stringValue).map { ids =>
                redis.cacheCategory(updatedProduct.category, ids :+ id)
              }
            } else Future.successful(())
          }.getOrElse(Future.successful(()))
          _ <- redis.cacheProduct(updatedProduct)
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
            categoryIds <- redis.getCachedCategoryIds(product.category.stringValue)
            updatedIds = categoryIds.filterNot(_ == id)
            allIds <- redis.getCachedProductIds
            _ <- redis.cacheCategory(product.category, updatedIds)
            _ <- redis.cacheAllProductsIds(allIds.filterNot(_ == id))
            _ <- redis.removeProductCache(id)
            deleted <- productRepository.delete(id)
          } yield deleted
        case None =>
          Future.successful(false)
      }
    } yield isDeleted
  }

  override def findAll(): Future[Seq[Product]] = {
    for {
      productIds <- redis.getCachedProductIds
      products <- if (productIds.nonEmpty) {
        Future.sequence(productIds.map(redis.getCachedProduct))
          .map(_.flatten)
      } else {
        for {
          products <- productRepository.findAll()
          _ <- Future.sequence(products.map { product =>
            redis.cacheProduct(product)
          })
          _ <- redis.cacheAllProductsIds(products.map(_.id.get))
          _ <- Future.sequence(products.groupBy(_.category).map { case (category, categoryProducts) =>
            redis.cacheCategory(category, categoryProducts.map(_.id.get))
          })
        } yield products
      }
    } yield products
  }
}