package modules

import com.google.inject.{AbstractModule, Provides}
import repositories.{CachedProductRepository, DBProductRepository, ProductRepository}
import services.{ProductService, ProductServiceImpl}

import javax.inject.Named

class ProductModule extends AbstractModule {
  
  @Provides
  @Named("db")
  def dbRepositoryProvider(DBProductRepository: DBProductRepository): ProductRepository = {
    DBProductRepository
  }
  
  override def configure(): Unit = {

    bind(classOf[ProductRepository])
      .to(classOf[CachedProductRepository])
      .asEagerSingleton()

    bind(classOf[ProductService])
      .to(classOf[ProductServiceImpl])
      .asEagerSingleton()
  }
}