package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.mongodb.ConnectionString;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.AnnouncementSeverity;
import tacos.OpsAnnouncement;
import tacos.api.dto.AnnouncementRequest;
import tacos.data.OpsAnnouncementRepository;

//TC-33 - persistencia y limite concurrente reales en Mongo
@Testcontainers(disabledWithoutDocker = true)
public class Tc33MongoAnnouncementIntegrationTest {
  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
  @Container
  private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:4.4.29");
  private static MongoClient client;
  private static ReactiveMongoTemplate template;
  private static OpsAnnouncementRepository repository;

  @BeforeAll
  public static void setup() {
    client = MongoClients.create(new ConnectionString(MONGO.getReplicaSetUrl()));
    template = new ReactiveMongoTemplate(new SimpleReactiveMongoDatabaseFactory(client, "tc33"));
    repository = new ReactiveMongoRepositoryFactory(template)
        .getRepository(OpsAnnouncementRepository.class);
  }

  @BeforeEach
  public void resetCollection() {
    StepVerifier.create(template.dropCollection(OpsAnnouncement.class)
        .onErrorResume(error -> Mono.empty())
        .then(template.createCollection(OpsAnnouncement.class))
        .then(template.indexOps(OpsAnnouncement.class).ensureIndex(
            new Index().on("slot", Direction.ASC).unique().sparse())))
        .expectNextCount(1).verifyComplete();
  }

  @AfterAll
  public static void close() {
    if (client != null) {
      client.close();
    }
  }

  @Test
  public void shouldPersistReadAndDeleteByStableIdWithoutExposingAuthor() {
    OpsAnnouncementService service = service(2);
    StepVerifier.create(service.create(request("Planned maintenance"), "admin-test")
        .flatMap(created -> repository.findById(created.getId())
            .doOnNext(stored -> assertThat(stored.getCreatedBy()).isEqualTo("admin-test"))
            .then(service.active().single())
            .doOnNext(response -> assertThat(response.getText()).isEqualTo("Planned maintenance"))
            .then(service.delete(created.getId()))
            .then(repository.count())))
        .expectNext(0L).verifyComplete();
  }

  @Test
  public void shouldAllowOnlyOneConcurrentAnnouncementWhenLimitIsOne() {
    OpsAnnouncementService service = service(1);
    Mono<Boolean> first = service.create(request("First"), "admin-test")
        .map(value -> true).onErrorReturn(false);
    Mono<Boolean> second = service.create(request("Second"), "admin-test")
        .map(value -> true).onErrorReturn(false);

    StepVerifier.create(Mono.zip(first, second)
        .doOnNext(result -> assertThat(result.getT1() ^ result.getT2()).isTrue())
        .then(repository.count()))
        .expectNext(1L).verifyComplete();
  }

  private OpsAnnouncementService service(int maxActive) {
    return new OpsAnnouncementService(repository, Clock.fixed(NOW, ZoneOffset.UTC), maxActive);
  }

  private AnnouncementRequest request(String text) {
    AnnouncementRequest request = new AnnouncementRequest();
    request.setText(text);
    request.setSeverity(AnnouncementSeverity.INFO);
    request.setExpiresAt(NOW.plusSeconds(3600));
    return request;
  }
}
//Fin TC-33
