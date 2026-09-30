package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.OrderStatus;
import tacos.TacoOrder;
import tacos.User;
import tacos.api.error.ConflictException;
import tacos.data.OrderRepository;

//TC-25 - Matriz, idempotencia, ownership y corte de cancelacion
public class OrderWorkflowServiceTest {
  private OrderRepository orderRepo;
  private InventoryService inventoryService;
  private OrderWorkflowService service;

  @BeforeEach
  public void setup() {
    orderRepo = Mockito.mock(OrderRepository.class);
    inventoryService = Mockito.mock(InventoryService.class);
    service = new OrderWorkflowService(orderRepo, inventoryService);
  }

  @Test
  public void shouldTransitionAndAuditKitchenActor() {
    TacoOrder order = order(OrderStatus.CREATED, "alice");
    when(orderRepo.findById("O1")).thenReturn(Mono.just(order));
    when(orderRepo.save(order)).thenReturn(Mono.just(order));

    StepVerifier.create(service.transition("O1", OrderStatus.ACCEPTED, "start",
        auth("cook", "ROLE_KITCHEN")))
        .assertNext(saved -> {
          assertThat(saved.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
          assertThat(saved.getStatusHistory()).hasSize(1);
          assertThat(saved.getStatusHistory().get(0).getActor()).isEqualTo("cook");
        }).verifyComplete();
  }

  @Test
  public void shouldRejectInvalidTransitionWithoutSaving() {
    TacoOrder order = order(OrderStatus.CREATED, "alice");
    when(orderRepo.findById("O1")).thenReturn(Mono.just(order));

    StepVerifier.create(service.transition("O1", OrderStatus.READY, null,
        auth("cook", "ROLE_KITCHEN"))).expectError(ConflictException.class).verify();

    verify(orderRepo, never()).save(order);
  }

  @Test
  public void shouldKeepRepeatedTransitionIdempotent() {
    TacoOrder order = order(OrderStatus.ACCEPTED, "alice");
    when(orderRepo.findById("O1")).thenReturn(Mono.just(order));

    StepVerifier.create(service.transition("O1", OrderStatus.ACCEPTED, null,
        auth("cook", "ROLE_KITCHEN"))).expectNext(order).verifyComplete();

    verify(orderRepo, never()).save(order);
  }

  @Test
  public void shouldRejectLateCancellation() {
    TacoOrder order = order(OrderStatus.PREPARING, "alice");
    when(orderRepo.findById("O1")).thenReturn(Mono.just(order));

    StepVerifier.create(service.cancel("O1", auth("alice", "ROLE_USER")))
        .expectError(ConflictException.class).verify();
  }

  private TacoOrder order(OrderStatus status, String username) {
    TacoOrder order = new TacoOrder();
    order.setId("O1");
    order.setStatus(status);
    order.setUser(new User(username, "hash", "Name", "Street", "City", "ST",
        "00000", "555", username + "@test.invalid"));
    return order;
  }

  private UsernamePasswordAuthenticationToken auth(String name, String role) {
    return new UsernamePasswordAuthenticationToken(name, "n/a",
        Collections.singletonList(new SimpleGrantedAuthority(role)));
  }
}
//Fin TC-25
