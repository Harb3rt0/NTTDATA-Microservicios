package tacos.web.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.IdempotencyRecord;
import tacos.IdempotencyStatus;
import tacos.TacoOrder;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.error.BadRequestException;
import tacos.api.error.ConflictException;
import tacos.api.error.ResourceNotFoundException;
import tacos.data.IdempotencyRecordRepository;
import tacos.data.OrderRepository;

//TC-34 - coordina retries HTTP sin duplicar orden, reserva ni outbox
@Service
public class IdempotentOrderService {
  private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9._:-]{8,128}");
  private final IdempotencyRecordRepository records;
  private final OrderRepository orders;
  private final OrderService orderService;
  private final CanonicalOrderHasher hasher;
  private final Clock clock;

  public IdempotentOrderService(IdempotencyRecordRepository records, OrderRepository orders,
      OrderService orderService, CanonicalOrderHasher hasher, Clock clock) {
    this.records = records;
    this.orders = orders;
    this.orderService = orderService;
    this.hasher = hasher;
    this.clock = clock;
  }

  public Mono<TacoOrder> create(String key, OrderCreateRequest request,
      Authentication authentication, String correlationId) {
    if (!VALID_KEY.matcher(key).matches()) {
      return Mono.error(new BadRequestException("INVALID_IDEMPOTENCY_KEY",
          "Idempotency-Key has an invalid format."));
    }
    String user = authentication.getName();
    String hash = hasher.hash(request);
    return records.findByUserIdAndKey(user, key)
        .flatMap(record -> resolveExisting(record, hash))
        .switchIfEmpty(Mono.defer(() -> acquire(user, key, hash)
            .flatMap(record -> orderService.createOrder(request, authentication, correlationId)
                .flatMap(order -> complete(record, order))
                .onErrorResume(error -> fail(record).then(Mono.error(error))))
            .onErrorResume(DuplicateKeyException.class,
                error -> awaitCompleted(user, key, hash))));
  }

  private Mono<IdempotencyRecord> acquire(String user, String key, String hash) {
    Instant now = clock.instant();
    IdempotencyRecord record = new IdempotencyRecord();
    record.setUserId(user);
    record.setKey(key);
    record.setRequestHash(hash);
    record.setStatus(IdempotencyStatus.IN_PROGRESS);
    record.setCreatedAt(now);
    record.setUpdatedAt(now);
    record.setExpiresAt(now.plus(Duration.ofDays(7)));
    return records.save(record);
  }

  private Mono<TacoOrder> complete(IdempotencyRecord record, TacoOrder order) {
    record.setOrderId(order.getId());
    record.setStatus(IdempotencyStatus.COMPLETED);
    record.setUpdatedAt(clock.instant());
    return records.save(record).thenReturn(order);
  }

  private Mono<Void> fail(IdempotencyRecord record) {
    record.setStatus(IdempotencyStatus.FAILED);
    record.setUpdatedAt(clock.instant());
    return records.save(record).then();
  }

  private Mono<TacoOrder> replay(IdempotencyRecord record, String hash) {
    if (!hash.equals(record.getRequestHash())) {
      return Mono.error(new ConflictException("IDEMPOTENCY_KEY_REUSED",
          "Idempotency-Key was already used with a different request."));
    }
    if (record.getStatus() == IdempotencyStatus.COMPLETED) {
      return orders.findById(record.getOrderId()).switchIfEmpty(Mono.error(
          new ResourceNotFoundException("IDEMPOTENT_ORDER_NOT_FOUND", "Stored order was not found.")));
    }
    return Mono.empty();
  }

  //TC-34 - resuelve estados persistentes sin volver a ejecutar efectos
  private Mono<TacoOrder> resolveExisting(IdempotencyRecord record, String hash) {
    if (!hash.equals(record.getRequestHash())) {
      return replay(record, hash);
    }
    if (record.getStatus() == IdempotencyStatus.COMPLETED) {
      return replay(record, hash);
    }
    if (record.getStatus() == IdempotencyStatus.FAILED) {
      return Mono.error(new ConflictException("IDEMPOTENCY_PREVIOUSLY_FAILED",
          "The original request failed; use a new Idempotency-Key."));
    }
    return awaitCompleted(record.getUserId(), record.getKey(), hash);
  }
  //Fin TC-34

  private Mono<TacoOrder> awaitCompleted(String user, String key, String hash) {
    return Flux.interval(Duration.ZERO, Duration.ofMillis(50)).take(40)
        .concatMap(tick -> records.findByUserIdAndKey(user, key))
        .flatMap(record -> {
          if (record.getStatus() == IdempotencyStatus.FAILED) {
            return Mono.error(new ConflictException("IDEMPOTENCY_PREVIOUSLY_FAILED",
                "The original request failed; use a new Idempotency-Key."));
          }
          return replay(record, hash);
        }).next()
        .switchIfEmpty(Mono.error(new ConflictException("IDEMPOTENCY_IN_PROGRESS",
            "The original request is still in progress.")));
  }
}
//Fin TC-34
