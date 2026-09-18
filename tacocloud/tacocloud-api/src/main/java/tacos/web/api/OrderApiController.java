package tacos.web.api;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import org.springframework.web.server.ResponseStatusException;
import tacos.TacoOrder;
import tacos.data.OrderRepository;
import tacos.messaging.OrderMessagingService;
import tacos.web.api.dto.OrderPatchRequest;

@RestController
@RequestMapping(path = "/api/orders", produces = "application/json")
@CrossOrigin(origins = "http://localhost:8080")
public class OrderApiController {

  private OrderRepository repo;
  private OrderMessagingService orderMessages;
  private EmailOrderService emailOrderService;

  public OrderApiController(OrderRepository repo,
      OrderMessagingService orderMessages,
      EmailOrderService emailOrderService) {
    this.repo = repo;
    this.orderMessages = orderMessages;
    this.emailOrderService = emailOrderService;
  }

  @GetMapping(produces = "application/json")
  public Flux<TacoOrder> allOrders() {
    return repo.findAll();
  }

  // @PostMapping(consumes="application/json")
  // @ResponseStatus(HttpStatus.CREATED)
  // public Mono<Order> postOrder(@RequestBody Mono<Order> order) {
  // order.subscribe(orderMessages::sendOrder); // TODO: not ideal...work into
  // reactive flow below
  // return order
  // .flatMap(repo::save);
  // }

  @PostMapping(consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<TacoOrder> postOrder(@RequestBody TacoOrder order) {
    orderMessages.sendOrder(order);
    return repo.save(order);
  }

  @PostMapping(path = "fromEmail", consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<TacoOrder> postOrderFromEmail(@RequestBody Mono<EmailOrder> emailOrder) {
    Mono<TacoOrder> order = emailOrderService.convertEmailOrderToDomainOrder(emailOrder);
    order.subscribe(orderMessages::sendOrder); // TODO: not ideal...work into reactive flow below
    return order
        .flatMap(repo::save);
  }

  /*
   * TC-05 - PUT y DELETE de ordenes con identidad consistente
   * caso PUT
   */
  @PutMapping(path = "/{orderId}", consumes = "application/json")
  public Mono<ResponseEntity<TacoOrder>> putOrder(@PathVariable("orderId") String orderId,
      @RequestBody TacoOrder order) {
    return repo.findById(orderId)
        .map(existingOrder -> {
          existingOrder.setDeliveryName(order.getDeliveryName());
          existingOrder.setDeliveryStreet(order.getDeliveryStreet());
          existingOrder.setDeliveryCity(order.getDeliveryCity());
          existingOrder.setDeliveryState(order.getDeliveryState());
          existingOrder.setDeliveryZip(order.getDeliveryZip());
          existingOrder.setTacos(order.getTacos());
          return existingOrder;
        })
        .flatMap(repo::save)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }
  // TC-05 - Fin caso PUT

  // TC-04 - PATCH de ordenes con lista blanca y sin ZIP mutante
  @PatchMapping(path = "/{orderId}", consumes = "application/json")
  public Mono<TacoOrder> patchOrder(@PathVariable("orderId") String orderId,
      @RequestBody OrderPatchRequest patch) {

    return repo.findById(orderId)
        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
        .map(order -> {
          if (patch.getDeliveryName() != null) {
            order.setDeliveryName(patch.getDeliveryName());
          }
          if (patch.getDeliveryStreet() != null) {
            order.setDeliveryStreet(patch.getDeliveryStreet());
          }
          if (patch.getDeliveryCity() != null) {
            order.setDeliveryCity(patch.getDeliveryCity());
          }
          if (patch.getDeliveryState() != null) {
            order.setDeliveryState(patch.getDeliveryState());
          }
          if (patch.getDeliveryZip() != null) {
            order.setDeliveryZip(patch.getDeliveryZip());
          }
          return order;
        })
        .flatMap(repo::save);
  }
  // TC-04 - Fin

  /*
   * TC-05 - PUT y DELETE de ordenes con identidad consistente
   * caso DELETE
   */
  @DeleteMapping("/{orderId}")
  public Mono<ResponseEntity<Void>> deleteOrder(@PathVariable("orderId") String orderId) {
    return repo.findById(orderId)
        .flatMap(order -> repo.deleteById(order.getId())
            .then(Mono.just(ResponseEntity.noContent().<Void>build())))
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }
  // TC-05 - Fin caso DELETE
}
