package tacos.web.api;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.TacoOrder;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.dto.OrderPatchRequest;
import tacos.api.dto.OrderResponse;
import tacos.api.dto.OrderUpdateRequest;
import tacos.api.mapper.OrderMapper;
import tacos.data.OrderRepository;
import tacos.messaging.OrderMessagingService;

@RestController
@Validated
@RequestMapping(path = "/api/orders", produces = "application/json")
public class OrderApiController {

  private final OrderMapper orderMapper;
  private OrderRepository repo;
  private OrderMessagingService orderMessages;
  private EmailOrderService emailOrderService;
  private OrderService orderService;

  public OrderApiController(OrderRepository repo,
      OrderMessagingService orderMessages,
      EmailOrderService emailOrderService,
      OrderService orderService, OrderMapper orderMapper) {
    this.repo = repo;
    this.orderMessages = orderMessages;
    this.emailOrderService = emailOrderService;
    this.orderService = orderService;
    this.orderMapper = orderMapper;
  }

  @GetMapping(produces = "application/json")
  public Flux<OrderResponse> allOrders(Authentication authentication) {
    //modificacion para TC-11
    return orderService.findOrders(authentication).map(orderMapper::toResponse);
  }

  // @PostMapping(consumes="application/json")
  // @ResponseStatus(HttpStatus.CREATED)
  // public Mono<Order> postOrder(@RequestBody Mono<Order> order) {
  // order.subscribe(orderMessages::sendOrder); // TODO: not ideal...work into
  // reactive flow below
  // return order
  // .flatMap(repo::save);
  // }

  // @PostMapping(consumes = "application/json")
  // @ResponseStatus(HttpStatus.CREATED)
  // public Mono<TacoOrder> postOrder(@RequestBody TacoOrder order) {
  //   orderMessages.sendOrder(order);
  //   return repo.save(order);
  // }

  //TC-08 - Separar DTOs de entrada, respuesta y persistencia
  @PostMapping(consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<OrderResponse> postOrder(@Valid @RequestBody OrderCreateRequest request,
      Authentication authentication) {
    //modificacion para TC-11
    return orderService.createOrder(request, authentication)
      .map(orderMapper::toResponse);
  }
  //TC-08 - Fin

  // @PostMapping(path = "fromEmail", consumes = "application/json")
  // @ResponseStatus(HttpStatus.CREATED)
  // public Mono<TacoOrder> postOrderFromEmail(@RequestBody Mono<EmailOrder> emailOrder) {
  //   Mono<TacoOrder> order = emailOrderService.convertEmailOrderToDomainOrder(emailOrder);
  //   order.subscribe(orderMessages::sendOrder); // TODO: not ideal...work into reactive flow below
  //   return order
  //       .flatMap(repo::save);
  // }

  //TC-06 - Convertir ordenes de correo sin carreras ni nulls sorpresa
  @PostMapping(path = "fromEmail", consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  //modificacion para TC-08
  public Mono<OrderResponse> postOrderFromEmail(@Valid @RequestBody EmailOrder emailOrder) {
    //TC-07 - Una sola suscripcion para guarar y publicar
    return emailOrderService.convertEmailOrderToDomainOrder(Mono.just(emailOrder))
      .flatMap(orderService::saveAndPublish)
      .map(orderMapper::toResponse);
      // .flatMap(savedOrder ->
      //   Mono.fromRunnable(() -> orderMessages.sendOrder(savedOrder))
      //   .thenReturn(savedOrder)
      // );
    //TC-07 - Fin
  }
  //TC-06 - Fin

  /*
   * TC-05 - PUT y DELETE de ordenes con identidad consistente
   * caso PUT
   */
  @PutMapping(path = "/{orderId}", consumes = "application/json")
  public Mono<ResponseEntity<OrderResponse>> putOrder(
      @PathVariable("orderId") @NotBlank @Size(max = 64) String orderId,
      @Valid @RequestBody OrderUpdateRequest order, Authentication authentication) {
    //modificacion para TC-11
    return orderService
        .updateOrder(orderId, order, authentication)
        .map(orderMapper::toResponse)
        .map(ResponseEntity::ok);
  }
  // TC-05 - Fin caso PUT

  // TC-04 - PATCH de ordenes con lista blanca y sin ZIP mutante
  @PatchMapping(path = "/{orderId}", consumes = "application/json")
  //adaptacion para TC-08
  public Mono<OrderResponse> patchOrder(
      @PathVariable("orderId") @NotBlank @Size(max = 64) String orderId,
      @Valid @RequestBody OrderPatchRequest patch, Authentication authentication) {

    //modificacion para TC-11
    return orderService.patchOrder(orderId, patch, authentication)
        .map(orderMapper::toResponse);
  }
  // TC-04 - Fin

  /*
   * TC-05 - PUT y DELETE de ordenes con identidad consistente
   * caso DELETE
   */
  @DeleteMapping("/{orderId}")
  public Mono<ResponseEntity<Void>> deleteOrder(
      @PathVariable("orderId") @NotBlank @Size(max = 64) String orderId,
      Authentication authentication) {
    //modificacion para TC-11
    return orderService.deleteOrder(orderId, authentication)
        .thenReturn(ResponseEntity.noContent().<Void>build());
  }
  // TC-05 - Fin caso DELETE
}
