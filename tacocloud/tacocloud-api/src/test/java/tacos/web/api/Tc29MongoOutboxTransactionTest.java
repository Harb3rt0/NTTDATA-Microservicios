package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.ConnectionString;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.OutboxEvent;
import tacos.OutboxStatus;
import tacos.TacoOrder;

//TC-29 - Prueba real de rollback conjunto con replica set
@Testcontainers(disabledWithoutDocker = true)
public class Tc29MongoOutboxTransactionTest {
  @Container
  private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:4.4.29");

  private static MongoClient client;
  private static ReactiveMongoTemplate template;
  private static TransactionalOperator transactions;

  @BeforeAll
  public static void setup() {
    client = MongoClients.create(new ConnectionString(MONGO.getReplicaSetUrl()));
    SimpleReactiveMongoDatabaseFactory factory =
        new SimpleReactiveMongoDatabaseFactory(client, "tc29");
    template = new ReactiveMongoTemplate(factory);
    transactions = TransactionalOperator.create(new ReactiveMongoTransactionManager(factory));
  }

  @AfterAll
  public static void close() {
    if (client != null) {
      client.close();
    }
  }

  @Test
  public void shouldCommitOrderAndNewOutboxTogether() {
    TacoOrder order = new TacoOrder();
    order.setId("ORDER-COMMIT");
    OutboxEvent outbox = new OutboxEvent();
    outbox.setId("OUTBOX-COMMIT");
    outbox.setEventId("EVENT-COMMIT");
    outbox.setStatus(OutboxStatus.NEW);
    outbox.setPayload("{\"eventId\":\"EVENT-COMMIT\",\"version\":1,"
        + "\"type\":\"CREATED\",\"payload\":{\"orderId\":\"ORDER-COMMIT\"}}");

    StepVerifier.create(transactions.transactional(
        template.save(order).then(template.save(outbox)).then()))
        .verifyComplete();
    StepVerifier.create(template.findById("ORDER-COMMIT", TacoOrder.class))
        .expectNextCount(1).verifyComplete();
    StepVerifier.create(template.findById("OUTBOX-COMMIT", OutboxEvent.class))
        .assertNext(saved -> {
          assertThat(saved.getStatus()).isEqualTo(OutboxStatus.NEW);
          assertThat(saved.getPayload()).doesNotContain("password", "paymentToken", "cvv", "pan");
        }).verifyComplete();
  }

  @Test
  public void shouldRollbackOrderWhenOutboxWriteFails() {
    TacoOrder order = new TacoOrder();
    order.setId("ORDER-ROLLBACK");
    OutboxEvent outbox = new OutboxEvent();
    outbox.setId("OUTBOX-ROLLBACK");
    outbox.setEventId("EVENT-ROLLBACK");
    outbox.setStatus(OutboxStatus.NEW);

    Mono<Void> work = template.save(order).then(template.save(outbox))
        .then(Mono.error(new IllegalStateException("forced rollback")));
    StepVerifier.create(transactions.transactional(work))
        .expectError(IllegalStateException.class).verify();

    StepVerifier.create(template.count(
        org.springframework.data.mongodb.core.query.Query.query(
            org.springframework.data.mongodb.core.query.Criteria.where("_id").is("ORDER-ROLLBACK")),
        TacoOrder.class))
        .assertNext(count -> assertThat(count).isZero()).verifyComplete();
    StepVerifier.create(template.count(
        org.springframework.data.mongodb.core.query.Query.query(
            org.springframework.data.mongodb.core.query.Criteria.where("_id").is("OUTBOX-ROLLBACK")),
        OutboxEvent.class))
        .assertNext(count -> assertThat(count).isZero()).verifyComplete();
  }
}
//Fin TC-29
