package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.IdempotencyRecord;
import tacos.IdempotencyStatus;
import tacos.TacoOrder;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.error.ConflictException;
import tacos.data.IdempotencyRecordRepository;
import tacos.data.OrderRepository;

//TC-34 - replay, conflicto y scope por usuario
public class Tc34IdempotentOrderServiceTest {
  @Test
  public void shouldReplayCompletedOrderWithoutCreatingAgain() {
    IdempotencyRecordRepository records = mock(IdempotencyRecordRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    OrderService orderService = mock(OrderService.class);
    CanonicalOrderHasher hasher = mock(CanonicalOrderHasher.class);
    OrderCreateRequest request = mock(OrderCreateRequest.class);
    TacoOrder order = new TacoOrder();
    order.setId("O1");
    IdempotencyRecord completed = completed("alice", "key-12345", "hash", "O1");
    when(hasher.hash(request)).thenReturn("hash");
    when(records.findByUserIdAndKey("alice", "key-12345")).thenReturn(Mono.just(completed));
    when(orders.findById("O1")).thenReturn(Mono.just(order));

    StepVerifier.create(service(records, orders, orderService, hasher)
        .create("key-12345", request, auth("alice"), "CORR"))
        .expectNext(order).verifyComplete();
    verify(orderService, times(0)).createOrder(any(), any(), any());
  }

  @Test
  public void shouldRejectSameKeyWithDifferentCanonicalBody() {
    IdempotencyRecordRepository records = mock(IdempotencyRecordRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    OrderService orderService = mock(OrderService.class);
    CanonicalOrderHasher hasher = mock(CanonicalOrderHasher.class);
    OrderCreateRequest request = mock(OrderCreateRequest.class);
    when(hasher.hash(request)).thenReturn("new-hash");
    when(records.findByUserIdAndKey("alice", "key-12345"))
        .thenReturn(Mono.just(completed("alice", "key-12345", "old-hash", "O1")));

    StepVerifier.create(service(records, orders, orderService, hasher)
        .create("key-12345", request, auth("alice"), "CORR"))
        .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(ConflictException.class)
            .hasMessageContaining("different request"))
        .verify();
  }

  @Test
  public void shouldScopeTheSameKeyIndependentlyByAuthenticatedUser() {
    IdempotencyRecordRepository records = mock(IdempotencyRecordRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    OrderService orderService = mock(OrderService.class);
    CanonicalOrderHasher hasher = mock(CanonicalOrderHasher.class);
    OrderCreateRequest request = mock(OrderCreateRequest.class);
    TacoOrder order = new TacoOrder();
    order.setId("O-B");
    when(hasher.hash(request)).thenReturn("hash");
    when(records.findByUserIdAndKey("bob", "key-12345")).thenReturn(Mono.empty());
    when(records.save(any(IdempotencyRecord.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(orderService.createOrder(eq(request), any(), eq("CORR-B"))).thenReturn(Mono.just(order));

    StepVerifier.create(service(records, orders, orderService, hasher)
        .create("key-12345", request, auth("bob"), "CORR-B"))
        .expectNext(order).verifyComplete();
    verify(records).findByUserIdAndKey("bob", "key-12345");
    verify(orderService).createOrder(eq(request), any(), eq("CORR-B"));
  }

  private IdempotentOrderService service(IdempotencyRecordRepository records,
      OrderRepository orders, OrderService orderService, CanonicalOrderHasher hasher) {
    return new IdempotentOrderService(records, orders, orderService, hasher,
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
  }

  private IdempotencyRecord completed(String user, String key, String hash, String orderId) {
    IdempotencyRecord record = new IdempotencyRecord();
    record.setUserId(user);
    record.setKey(key);
    record.setRequestHash(hash);
    record.setOrderId(orderId);
    record.setStatus(IdempotencyStatus.COMPLETED);
    return record;
  }

  private Authentication auth(String username) {
    return new UsernamePasswordAuthenticationToken(username, "n/a", Collections.emptyList());
  }
}
//Fin TC-34
