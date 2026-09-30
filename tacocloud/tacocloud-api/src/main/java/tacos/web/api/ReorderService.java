package tacos.web.api;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.OrderLine;
import tacos.ReorderOperation;
import tacos.TacoOrder;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.dto.OrderLineCreateRequest;
import tacos.api.dto.ReorderRequest;
import tacos.api.dto.ReorderResponse;
import tacos.api.dto.TacoCreateRequest;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.ConflictException;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.OrderMapper;
import tacos.data.OrderRepository;

//TC-24 - Recompra reconstruida y revalidada con idempotencia propia
@Service
public class ReorderService {
    private final OrderRepository orderRepo;
    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final ReactiveMongoTemplate mongoTemplate;
    private final Clock clock;

    public ReorderService(OrderRepository orderRepo, OrderService orderService, OrderMapper orderMapper,
            ReactiveMongoTemplate mongoTemplate, Clock clock) {
        this.orderRepo = orderRepo;
        this.orderService = orderService;
        this.orderMapper = orderMapper;
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public Mono<ReorderResponse> reorder(String orderId, ReorderRequest request,
            Authentication authentication) {
        String username = username(authentication);
        String operationId = username + ":" + orderId + ":" + request.getReorderKey();
        return completed(operationId, orderId).switchIfEmpty(orderRepo.findById(orderId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                ApiErrorCodes.ORDER_NOT_FOUND, "Order was not found.")))
            .flatMap(original -> {
                if (original.getUser() == null || !username.equals(original.getUser().getUsername())) {
                    return Mono.error(new ResourceNotFoundException(
                        ApiErrorCodes.ORDER_NOT_FOUND, "Order was not found."));
                }
                OrderCreateRequest command = command(original, request.getPaymentMethodId());
                return orderService.prepareOrder(command, authentication)
                    .flatMap(prepared -> decide(original, prepared, request, operationId));
            }));
    }

    private Mono<ReorderResponse> decide(TacoOrder original, TacoOrder prepared,
            ReorderRequest request, String operationId) {
        boolean priceChanged = original.getTotal().compareTo(prepared.getTotal()) != 0;
        if (priceChanged && !request.isConfirmPriceChange()) {
            return Mono.just(response(original, prepared, true, true, null,
                "Current price differs; send confirmPriceChange=true to create the order."));
        }
        ReorderOperation operation = new ReorderOperation();
        operation.setId(operationId);
        operation.setUserId(original.getUser().getId());
        operation.setOriginalOrderId(original.getId());
        operation.setReorderKey(request.getReorderKey());
        operation.setCreatedAt(Instant.now(clock));
        return mongoTemplate.insert(operation)
            .flatMap(saved -> orderService.createPreparedOrder(prepared, "reorder:" + operationId)
                .flatMap(created -> mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(operationId)),
                    Update.update("newOrderId", created.getId()), ReorderOperation.class)
                    .thenReturn(response(original, prepared, priceChanged, false, created,
                        "Reorder created with current catalog, coupon and inventory rules.")))
                .onErrorResume(error -> mongoTemplate.remove(Query.query(
                    Criteria.where("_id").is(operationId)), ReorderOperation.class)
                    .then(Mono.error(error))))
            .onErrorResume(DuplicateKeyException.class, error -> completed(operationId, original.getId())
                .switchIfEmpty(Mono.error(new ConflictException(ApiErrorCodes.REORDER_IN_PROGRESS,
                    "A reorder with this key is still in progress."))));
    }

    private Mono<ReorderResponse> completed(String operationId, String originalOrderId) {
        return mongoTemplate.findById(operationId, ReorderOperation.class)
            .filter(operation -> operation.getNewOrderId() != null)
            .flatMap(operation -> Mono.zip(orderRepo.findById(originalOrderId),
                orderRepo.findById(operation.getNewOrderId())))
            .map(orders -> new ReorderResponse(originalOrderId,
                orders.getT1().getTotal().compareTo(orders.getT2().getTotal()) != 0, false,
                orders.getT1().getTotal(), orders.getT2().getTotal(), orders.getT2().getCurrency(),
                "Existing idempotent reorder returned.", orderMapper.toResponse(orders.getT2())));
    }

    private OrderCreateRequest command(TacoOrder original, String paymentMethodId) {
        OrderCreateRequest command = new OrderCreateRequest();
        command.setDeliveryName(original.getDeliveryName());
        command.setDeliveryStreet(original.getDeliveryStreet());
        command.setDeliveryCity(original.getDeliveryCity());
        command.setDeliveryState(original.getDeliveryState());
        command.setDeliveryZip(original.getDeliveryZip());
        command.setPaymentMethodId(paymentMethodId);
        command.setCouponCode(original.getCouponCode());
        command.setItems(original.getItems().stream().map(this::line).collect(Collectors.toList()));
        return command;
    }

    private OrderLineCreateRequest line(OrderLine original) {
        TacoCreateRequest taco = new TacoCreateRequest();
        taco.setName(original.getTaco().getName());
        taco.setIngredientIds(original.getTaco().getIngredients().stream()
            .map(ingredient -> ingredient.getId()).collect(Collectors.toList()));
        OrderLineCreateRequest line = new OrderLineCreateRequest();
        line.setTaco(taco);
        line.setQuantity(original.getQuantity());
        return line;
    }

    private ReorderResponse response(TacoOrder original, TacoOrder current, boolean priceChanged,
            boolean confirmation, TacoOrder created, String message) {
        return new ReorderResponse(original.getId(), priceChanged, confirmation, original.getTotal(),
            current.getTotal(), current.getCurrency(), message,
            created == null ? null : orderMapper.toResponse(created));
    }

    private String username(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required.");
        }
        return authentication.getName();
    }
}
//Fin TC-24
