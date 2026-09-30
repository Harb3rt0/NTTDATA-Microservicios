package tacos.web.api;

import java.time.Duration;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OutboxEvent;
import tacos.OutboxStatus;
import tacos.messaging.OrderEvent;
import tacos.messaging.OrderMessagingService;

//TC-29 - Claim atomico, reintento acotado y recuperacion tras reinicio
@Component
public class OutboxPublisher {
  private final ReactiveMongoTemplate mongoTemplate;
  private final OrderMessagingService messagingService;
  private final ObjectMapper objectMapper;
  private final int batchSize;
  private final int maxAttempts;
  private final Duration retryDelay;
  private final Duration staleAfter;

  public OutboxPublisher(ReactiveMongoTemplate mongoTemplate, OrderMessagingService messagingService,
      ObjectMapper objectMapper, @Value("${tacocloud.outbox.batch-size:20}") int batchSize,
      @Value("${tacocloud.outbox.max-attempts:5}") int maxAttempts,
      @Value("${tacocloud.outbox.retry-delay:PT5S}") Duration retryDelay,
      @Value("${tacocloud.outbox.stale-after:PT1M}") Duration staleAfter) {
    this.mongoTemplate = mongoTemplate;
    this.messagingService = messagingService;
    this.objectMapper = objectMapper;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
    this.retryDelay = retryDelay;
    this.staleAfter = staleAfter;
  }

  public Flux<OutboxEvent> publishBatch() {
    return Flux.range(0, batchSize).concatMap(index -> claimOne())
        .concatMap(this::publishClaimed);
  }

  Mono<OutboxEvent> claimOne() {
    Date now = new Date();
    Date stale = Date.from(now.toInstant().minus(staleAfter));
    Criteria ready = new Criteria().orOperator(
        Criteria.where("status").is(OutboxStatus.NEW).and("nextAttemptAt").lte(now),
        Criteria.where("status").is(OutboxStatus.PUBLISHING).and("updatedAt").lte(stale));
    Query query = Query.query(ready).with(Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("_id")));
    Update update = new Update().set("status", OutboxStatus.PUBLISHING).set("updatedAt", now);
    return mongoTemplate.findAndModify(query, update,
        FindAndModifyOptions.options().returnNew(true), OutboxEvent.class);
  }

  private Mono<OutboxEvent> publishClaimed(OutboxEvent outbox) {
    return Mono.fromCallable(() -> objectMapper.readValue(outbox.getPayload(), OrderEvent.class))
        .doOnNext(messagingService::sendOrder)
        .flatMap(event -> markPublished(outbox))
        .onErrorResume(error -> markFailedAttempt(outbox, error));
  }

  private Mono<OutboxEvent> markPublished(OutboxEvent outbox) {
    Date now = new Date();
    Query query = Query.query(Criteria.where("_id").is(outbox.getId())
        .and("status").is(OutboxStatus.PUBLISHING));
    Update update = new Update().set("status", OutboxStatus.PUBLISHED)
        .set("publishedAt", now).set("updatedAt", now).unset("lastError");
    return mongoTemplate.findAndModify(query, update,
        FindAndModifyOptions.options().returnNew(true), OutboxEvent.class);
  }

  private Mono<OutboxEvent> markFailedAttempt(OutboxEvent outbox, Throwable error) {
    int attempts = outbox.getAttempts() + 1;
    OutboxStatus status = attempts >= maxAttempts ? OutboxStatus.FAILED : OutboxStatus.NEW;
    Date now = new Date();
    Date next = Date.from(now.toInstant().plus(retryDelay.multipliedBy(attempts)));
    String safeError = error.getClass().getSimpleName();
    Query query = Query.query(Criteria.where("_id").is(outbox.getId()));
    Update update = new Update().set("status", status).set("attempts", attempts)
        .set("updatedAt", now).set("nextAttemptAt", next).set("lastError", safeError);
    return mongoTemplate.findAndModify(query, update,
        FindAndModifyOptions.options().returnNew(true), OutboxEvent.class);
  }
}
//Fin TC-29
