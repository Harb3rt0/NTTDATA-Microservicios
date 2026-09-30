package tacos.kitchen;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

//TC-30 - Transaccion local del consumidor idempotente
@Configuration
public class KitchenMongoTransactionConfiguration {
  @Bean
  public MongoTransactionManager kitchenMongoTransactionManager(MongoDatabaseFactory databaseFactory) {
    return new MongoTransactionManager(databaseFactory);
  }
}
//Fin TC-30
