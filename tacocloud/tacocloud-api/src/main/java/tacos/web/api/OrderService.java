package tacos.web.api;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.dto.OrderUpdateRequest;
import tacos.api.dto.TacoCreateRequest;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.OrderMapper;
import tacos.api.mapper.TacoMapper;
import tacos.data.IngredientRepository;
import tacos.data.OrderRepository;
import tacos.messaging.OrderMessagingService;

@Service 
public class OrderService {
    private final OrderRepository repo;
    private final IngredientRepository ingredientRepo;
    private final OrderMessagingService orderMessages;
    private final TacoMapper tacoMapper;
    private final OrderMapper orderMapper;

    //modificacion para TC-08
    public OrderService(OrderRepository repo, IngredientRepository ingredientRepo, OrderMessagingService orderMessages, TacoMapper tacoMapper, OrderMapper orderMapper) {
        this.repo = repo;
        this.ingredientRepo = ingredientRepo;
        this.orderMessages = orderMessages;
        this.tacoMapper = tacoMapper;
        this.orderMapper = orderMapper;
    }

    //TC-07 - Una sola suscripcion para guarar y publicar
    public Mono<TacoOrder> saveAndPublish(TacoOrder order) {
        return repo.save(order)
            .flatMap(savedOrder ->
                Mono.fromRunnable(() -> orderMessages.sendOrder(savedOrder))
                .thenReturn(savedOrder)
            );
    }
    //TC-07 - Fin

    //TC-08 - Separar DTOs de entrada, respuesta y persistencia
    private Mono<Taco> resolveTaco(TacoCreateRequest request) {
        return Flux.fromIterable(request.getIngredientIds())
            .concatMap(ingredientId -> ingredientRepo.findById(ingredientId)
                .switchIfEmpty(Mono.error(new BusinessRuleException(
                    ApiErrorCodes.ORDER_INGREDIENT_NOT_FOUND,
                    "Ingredient '" + ingredientId + "' is not available.")))
            )
            .collectList()
            .map(ingredients -> tacoMapper.toEntity(request, ingredients));
    }

    public Mono<TacoOrder> createOrder(OrderCreateRequest request) {
        return Flux.fromIterable(request.getTacos())
            .concatMap(this::resolveTaco)
            .collectList()
            .map(tacos -> orderMapper.toEntity(request,tacos))
            .flatMap(this::saveAndPublish);
    }

    public Mono<TacoOrder> updateOrder(String orderId, OrderUpdateRequest request) {
        return repo.findById(orderId)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
            ApiErrorCodes.ORDER_NOT_FOUND, "Order was not found.")))
        .flatMap(existingOrder ->
            Flux.fromIterable(request.getTacos()).concatMap(this::resolveTaco)
                .collectList()
                .map(tacos -> { orderMapper.updateEntity(request, existingOrder, tacos);
                    return existingOrder;
                })
                .flatMap(repo::save)
        );
    }
    //TC-08 - fin
}
