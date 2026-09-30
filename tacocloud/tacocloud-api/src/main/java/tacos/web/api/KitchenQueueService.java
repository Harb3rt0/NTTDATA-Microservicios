package tacos.web.api;

import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.OrderStatusHistory;
import tacos.TacoOrder;
import tacos.api.dto.KitchenOrderItemResponse;
import tacos.api.dto.KitchenOrderResponse;

//TC-26 - Cola FIFO, claim atomico y ETA determinista
@Service
public class KitchenQueueService {
  private final ReactiveMongoTemplate mongoTemplate;
  private final int queueLimit;
  private final int baseMinutes;
  private final int queueMinutes;
  private final int quantityMinutes;
  private final int complexityMinutes;

  public KitchenQueueService(ReactiveMongoTemplate mongoTemplate,
      @Value("${tacocloud.kitchen.queue-limit:50}") int queueLimit,
      @Value("${tacocloud.kitchen.eta.base-minutes:5}") int baseMinutes,
      @Value("${tacocloud.kitchen.eta.queue-minutes:4}") int queueMinutes,
      @Value("${tacocloud.kitchen.eta.quantity-minutes:2}") int quantityMinutes,
      @Value("${tacocloud.kitchen.eta.complexity-minutes:1}") int complexityMinutes) {
    this.mongoTemplate = mongoTemplate;
    this.queueLimit = queueLimit;
    this.baseMinutes = baseMinutes;
    this.queueMinutes = queueMinutes;
    this.quantityMinutes = quantityMinutes;
    this.complexityMinutes = complexityMinutes;
  }

  public Flux<KitchenOrderResponse> queue(Authentication authentication) {
    return requireKitchen(authentication).thenMany(mongoTemplate.find(queueQuery(), TacoOrder.class))
        .index().map(tuple -> toResponse(tuple.getT2(), tuple.getT1()));
  }

  public Mono<KitchenOrderResponse> claim(Authentication authentication) {
    return requireKitchen(authentication).then(Mono.defer(() -> {
      String actor = authentication.getName();
      OrderStatusHistory audit = new OrderStatusHistory(OrderStatus.CREATED, OrderStatus.ACCEPTED,
          actor, new Date(), "KITCHEN_CLAIM", "Claimed from FIFO queue");
      Update update = new Update().set("status", OrderStatus.ACCEPTED)
          .set("stationId", actor).push("statusHistory", audit);
      return mongoTemplate.findAndModify(queueQuery(), update,
          FindAndModifyOptions.options().returnNew(true), TacoOrder.class);
    })).map(order -> toResponse(order, 0));
  }

  private Query queueQuery() {
    Query query = Query.query(Criteria.where("status").is(OrderStatus.CREATED));
    query.with(Sort.by(Sort.Order.asc("placedAt"), Sort.Order.asc("_id")));
    query.limit(queueLimit);
    return query;
  }

  private KitchenOrderResponse toResponse(TacoOrder order, long position) {
    KitchenOrderResponse response = new KitchenOrderResponse();
    response.setOrderId(order.getId());
    response.setPlacedAt(order.getPlacedAt());
    response.setStatus(order.getStatus());
    response.setStationId(order.getStationId());
    int quantity = order.getItems().stream().mapToInt(item -> item.getQuantity()).sum();
    int complexity = order.getItems().stream()
        .mapToInt(item -> item.getTaco().getIngredients().size()).sum();
    response.setEstimatedMinutes(baseMinutes + position * queueMinutes
        + (long) quantity * quantityMinutes + (long) complexity * complexityMinutes);
    response.setItems(order.getItems().stream().map(item -> {
      KitchenOrderItemResponse line = new KitchenOrderItemResponse();
      line.setName(item.getTaco().getName());
      line.setQuantity(item.getQuantity());
      line.setIngredients(item.getTaco().getIngredients().stream()
          .map(ingredient -> ingredient.getName()).collect(Collectors.toList()));
      return line;
    }).collect(Collectors.toList()));
    return response;
  }

  private Mono<Void> requireKitchen(Authentication authentication) {
    boolean permitted = authentication != null && authentication.isAuthenticated()
        && authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_KITCHEN".equals(authority.getAuthority()));
    return permitted ? Mono.empty()
        : Mono.error(new AccessDeniedException("Kitchen role is required."));
  }
}
//Fin TC-26
