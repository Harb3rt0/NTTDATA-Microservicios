package tacos.web.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.OrderLine;
import tacos.Taco;
import tacos.api.dto.OrderLineCreateRequest;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;
import tacos.api.mapper.TacoMapper;
import tacos.data.IngredientRepository;

//TC-14 - Calcula precios y cantidades confiables en el servidor
@Service
public class OrderPricingService {
    private static final int MONEY_SCALE = 2;

    private final IngredientRepository ingredientRepo;
    private final TacoMapper tacoMapper;
    private final int maxItemQuantity;
    private final String currency;

    public OrderPricingService(IngredientRepository ingredientRepo, TacoMapper tacoMapper,
            @Value("${tacocloud.order.max-item-quantity:10}") int maxItemQuantity,
            @Value("${tacocloud.pricing.currency:USD}") String currency) {
        this.ingredientRepo = ingredientRepo;
        this.tacoMapper = tacoMapper;
        this.maxItemQuantity = maxItemQuantity;
        this.currency = currency;
    }

    public Mono<OrderLine> priceItem(OrderLineCreateRequest request) {
        validateQuantity(request == null ? null : request.getQuantity());

        return Flux.fromIterable(request.getTaco().getIngredientIds())
            .concatMap(ingredientId -> ingredientRepo.findById(ingredientId)
                .switchIfEmpty(Mono.error(new BusinessRuleException(
                    ApiErrorCodes.ORDER_INGREDIENT_NOT_FOUND,
                    "Ingredient '" + ingredientId + "' is not available."))))
            .collectList()
            .map(ingredients -> priceResolvedTaco(
                tacoMapper.toEntity(request.getTaco(), ingredients), request.getQuantity()));
    }

    public Flux<OrderLine> priceItems(List<OrderLineCreateRequest> requests) {
        return Flux.defer(() -> {
            validateQuantities(requests); //modificacion para TC-18
            return Flux.fromIterable(requests).concatMap(this::priceItem);
        });
    }

    //TC-18 - Valida limites independientes antes de resolver ingredientes
    public void validateQuantities(List<OrderLineCreateRequest> requests) {
        requests.forEach(request -> validateQuantity(
            request == null ? null : request.getQuantity()));
    }
    //Fin TC-18

    public OrderLine priceResolvedTaco(Taco taco, int quantity) {
        validateQuantity(quantity);
        BigDecimal unitPrice = taco.getIngredients().stream()
            .map(this::ingredientPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        OrderLine item = new OrderLine();
        item.setTaco(taco);
        item.setQuantity(quantity);
        item.setUnitPriceAtPurchase(unitPrice);
        item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity))
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        return item;
    }

    public BigDecimal calculateTotal(List<OrderLine> items) {
        return items.stream()
            .map(OrderLine::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public String getCurrency() {
        return currency;
    }

    private BigDecimal ingredientPrice(Ingredient ingredient) {
        if (ingredient.getUnitPrice() == null) {
            throw new BusinessRuleException(ApiErrorCodes.INVALID_INGREDIENT_CATALOG,
                "Ingredient '" + ingredient.getId() + "' has no unit price.");
        }
        return ingredient.getUnitPrice();
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BusinessRuleException(ApiErrorCodes.INVALID_ITEM_QUANTITY,
                "Item quantity must be at least 1.");
        }
        if (quantity > maxItemQuantity) {
            throw new BusinessRuleException(ApiErrorCodes.ITEM_QUANTITY_LIMIT_EXCEEDED,
                "Item quantity cannot exceed " + maxItemQuantity + ".");
        }
    }
}
//Fin TC-14
