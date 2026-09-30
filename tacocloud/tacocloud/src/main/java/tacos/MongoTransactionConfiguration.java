package tacos;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

//TC-29 - Transaccion reactiva requerida para orden y outbox
@Configuration
public class MongoTransactionConfiguration {
  @Bean
  public ReactiveMongoTransactionManager reactiveMongoTransactionManager(
      ReactiveMongoDatabaseFactory databaseFactory) {
    return new ReactiveMongoTransactionManager(databaseFactory);
  }

  @Bean
  public TransactionalOperator transactionalOperator(
      ReactiveMongoTransactionManager transactionManager) {
    return TransactionalOperator.create(transactionManager);
  }
}
//Fin TC-29
