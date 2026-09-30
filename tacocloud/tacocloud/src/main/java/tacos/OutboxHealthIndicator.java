package tacos;

import java.time.Duration;
import java.util.Date;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

//TC-32 - health reactivo del backlog outbox sin bloquear
@Component("outbox")
public class OutboxHealthIndicator implements ReactiveHealthIndicator {
  private final ReactiveMongoTemplate mongoTemplate;

  public OutboxHealthIndicator(ReactiveMongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Mono<Health> health() {
    Date stale = Date.from(new Date().toInstant().minus(Duration.ofMinutes(5)));
    Query pending = Query.query(Criteria.where("status").in(OutboxStatus.NEW, OutboxStatus.PUBLISHING));
    Query failed = Query.query(Criteria.where("status").is(OutboxStatus.FAILED));
    Query old = Query.query(Criteria.where("status").in(OutboxStatus.NEW, OutboxStatus.PUBLISHING)
        .and("createdAt").lt(stale));
    return Mono.zip(mongoTemplate.count(pending, OutboxEvent.class),
            mongoTemplate.count(failed, OutboxEvent.class),
            mongoTemplate.count(old, OutboxEvent.class))
        .map(counts -> counts.getT2() > 0 || counts.getT3() > 0
            ? Health.down().withDetail("pending", counts.getT1())
                .withDetail("failed", counts.getT2()).withDetail("stale", counts.getT3()).build()
            : Health.up().withDetail("pending", counts.getT1()).build())
        .onErrorResume(error -> Mono.just(Health.down().withException(error).build()));
  }
}
//Fin TC-32
