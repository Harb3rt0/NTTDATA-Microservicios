package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Collections;

import com.mongodb.ConnectionString;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.IdempotencyRecord;
import tacos.TacoOrder;
import tacos.api.dto.OrderCreateRequest;
import tacos.data.IdempotencyRecordRepository;
import tacos.data.OrderRepository;

//TC-34 - concurrencia real respaldada por indice unico Mongo
@Testcontainers(disabledWithoutDocker = true)
public class Tc34MongoIdempotencyConcurrencyTest {
  @Container
  private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:4.4.29");
  private static MongoClient client;
  private static ReactiveMongoTemplate template;

  @BeforeAll
  public static void setup() {
    client = MongoClients.create(new ConnectionString(MONGO.getReplicaSetUrl()));
    template = new ReactiveMongoTemplate(new SimpleReactiveMongoDatabaseFactory(client, "tc34"));
    StepVerifier.create(template.indexOps(IdempotencyRecord.class).ensureIndex(
        new Index().on("userId", Direction.ASC).on("key", Direction.ASC).unique()))
        .expectNextCount(1).verifyComplete();
  }

  @AfterAll
  public static void close() {
    if (client != null) {
      client.close();
    }
  }

  @Test
  public void shouldCreateExactlyOnceForConcurrentSameUserKeyAndBody() {
    IdempotencyRecordRepository records = new ReactiveMongoRepositoryFactory(template)
        .getRepository(IdempotencyRecordRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    OrderService delegate = mock(OrderService.class);
    CanonicalOrderHasher hasher = mock(CanonicalOrderHasher.class);
    OrderCreateRequest request = mock(OrderCreateRequest.class);
    TacoOrder order = new TacoOrder();
    order.setId("ORDER-ONE");
    when(hasher.hash(request)).thenReturn("canonical-hash");
    when(delegate.createOrder(any(), any(), any())).thenReturn(Mono.just(order));
    when(orders.findById("ORDER-ONE")).thenReturn(Mono.just(order));
    IdempotentOrderService service = new IdempotentOrderService(records, orders, delegate,
        hasher, Clock.systemUTC());
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        "alice", "n/a", Collections.emptyList());

    StepVerifier.create(Mono.zip(
        service.create("same-key-123", request, authentication, "CORR-1"),
        service.create("same-key-123", request, authentication, "CORR-2")))
        .assertNext(pair -> assertThat(pair.getT1().getId()).isEqualTo(pair.getT2().getId()))
        .verifyComplete();

    verify(delegate, times(1)).createOrder(any(), any(), any());
    StepVerifier.create(records.count()).expectNext(1L).verifyComplete();
  }
}
//Fin TC-34
