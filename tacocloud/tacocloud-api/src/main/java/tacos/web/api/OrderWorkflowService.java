package tacos.web.api;

import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.OrderStatusHistory;
import tacos.TacoOrder;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.ConflictException;
import tacos.api.error.ResourceNotFoundException;
import tacos.data.OrderRepository;
import tacos.messaging.OrderEventType;

//TC-25 - Politica central del ciclo de vida de la orden
@Service
public class OrderWorkflowService {
  private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = transitions();

  private final OrderRepository orderRepo;
  private final InventoryService inventoryService;
  private final OrderOutboxService outboxService;
  private final TransactionalOperator transactionalOperator;
  private final BusinessMetrics businessMetrics;

  @Autowired
  public OrderWorkflowService(OrderRepository orderRepo, InventoryService inventoryService,
      OrderOutboxService outboxService, TransactionalOperator transactionalOperator,
      BusinessMetrics businessMetrics) { //modificacion para TC-32
    this.orderRepo = orderRepo;
    this.inventoryService = inventoryService;
    this.outboxService = outboxService;
    this.transactionalOperator = transactionalOperator;
    this.businessMetrics = businessMetrics;
  }

  OrderWorkflowService(OrderRepository orderRepo, InventoryService inventoryService) {
    this(orderRepo, inventoryService, null, null, null);
  }

  public Mono<TacoOrder> transition(String orderId, OrderStatus target, String reason,
      Authentication authentication) {
    return requireKitchen(authentication)
        .then(findOrder(orderId))
        .flatMap(order -> applyTransition(order, target, authentication.getName(), "KITCHEN", reason));
  }

  public Mono<TacoOrder> cancel(String orderId, Authentication authentication) {
    return findOrder(orderId)
        .flatMap(order -> {
          if (!hasRole(authentication, "ROLE_USER") || order.getUser() == null
              || !authentication.getName().equals(order.getUser().getUsername())) {
            return Mono.error(new AccessDeniedException("The order belongs to another user."));
          }
          if (order.getStatus() == OrderStatus.CANCELLED) {
            return Mono.just(order);
          }
          if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.ACCEPTED) {
            return Mono.error(new ConflictException(ApiErrorCodes.ORDER_CANNOT_BE_CANCELLED,
                "The order can no longer be cancelled."));
          }
          return applyTransition(order, OrderStatus.CANCELLED, authentication.getName(),
              "CUSTOMER", "Cancelled by customer")
              .flatMap(saved -> inventoryService.release(saved.getInventoryReservationKey())
                  .thenReturn(saved))
              .doOnSuccess(saved -> {
                if (businessMetrics != null) {
                  businessMetrics.orderCancelled();
                }
              }); //modificacion para TC-32
        });
  }

  private Mono<TacoOrder> applyTransition(TacoOrder order, OrderStatus target, String actor,
      String origin, String reason) {
    OrderStatus current = order.getStatus() == null ? OrderStatus.CREATED : order.getStatus();
    if (current == target) {
      return Mono.just(order);
    }
    if (!TRANSITIONS.getOrDefault(current, EnumSet.noneOf(OrderStatus.class)).contains(target)) {
      return Mono.error(new ConflictException(ApiErrorCodes.INVALID_ORDER_STATUS_TRANSITION,
          "The requested order status transition is not allowed."));
    }
    order.setStatus(target);
    order.getStatusHistory().add(new OrderStatusHistory(current, target, actor,
        new Date(), origin, reason));
    OrderEventType eventType = target == OrderStatus.CANCELLED
        ? OrderEventType.CANCELLED : OrderEventType.STATUS_CHANGED;
    Mono<TacoOrder> persistence = orderRepo.save(order);
    if (outboxService != null) {
      persistence = persistence.flatMap(saved -> outboxService.record(saved, eventType,
          java.util.UUID.randomUUID().toString()).thenReturn(saved));
    }
    return transactionalOperator == null ? persistence : transactionalOperator.transactional(persistence);
  }

  private Mono<TacoOrder> findOrder(String orderId) {
    return orderRepo.findById(orderId).switchIfEmpty(Mono.error(new ResourceNotFoundException(
        ApiErrorCodes.ORDER_NOT_FOUND, "Order was not found.")));
  }

  private Mono<Void> requireKitchen(Authentication authentication) {
    return hasRole(authentication, "ROLE_KITCHEN") ? Mono.empty()
        : Mono.error(new AccessDeniedException("Kitchen role is required."));
  }

  private boolean hasRole(Authentication authentication, String role) {
    return authentication != null && authentication.isAuthenticated()
        && authentication.getAuthorities().stream()
            .anyMatch(authority -> role.equals(authority.getAuthority()));
  }

  private static Map<OrderStatus, Set<OrderStatus>> transitions() {
    Map<OrderStatus, Set<OrderStatus>> map = new EnumMap<>(OrderStatus.class);
    map.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED));
    map.put(OrderStatus.ACCEPTED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
    map.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY));
    map.put(OrderStatus.READY, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY));
    map.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED));
    map.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
    map.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    return map;
  }
}
//Fin TC-25
