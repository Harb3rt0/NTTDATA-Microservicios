package tacos.web.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.api.dto.IngredientCatalogUpdateRequest;
import tacos.api.dto.IngredientRequest;
import tacos.api.dto.StockAdjustmentRequest;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.IngredientMapper;
import tacos.data.IngredientRepository;

//TC-13 - Caso de uso reactivo para catalogo e inventario
@Service
public class IngredientCatalogService {
    private static final int PRICE_SCALE = 2;
    private static final RoundingMode PRICE_ROUNDING = RoundingMode.HALF_UP;

    private final IngredientRepository ingredientRepo;
    private final IngredientMapper ingredientMapper;

    public IngredientCatalogService(IngredientRepository ingredientRepo, IngredientMapper ingredientMapper) {
        this.ingredientRepo = ingredientRepo;
        this.ingredientMapper = ingredientMapper;
    }

    public Mono<Ingredient> create(IngredientRequest request) {
        Ingredient ingredient = ingredientMapper.toEntity(request);
        normalizeAndValidate(ingredient);
        return ingredientRepo.save(ingredient);
    }

    public Mono<Ingredient> replace(String ingredientId, IngredientRequest request) {
        return findIngredient(ingredientId)
            .flatMap(existing -> {
                ingredientMapper.updateEntity(request, existing);
                normalizeAndValidate(existing);
                return ingredientRepo.save(existing);
            });
    }

    public Mono<Ingredient> updateCatalog(String ingredientId, IngredientCatalogUpdateRequest request) {
        return findIngredient(ingredientId)
            .flatMap(ingredient -> {
                ingredientMapper.updateCatalog(request, ingredient);
                normalizeAndValidate(ingredient);
                return ingredientRepo.save(ingredient);
            });
    }

    public Mono<Ingredient> adjustStock(String ingredientId, StockAdjustmentRequest request) {
        return findIngredient(ingredientId)
            .flatMap(ingredient -> {
                long adjustedStock = (long) ingredient.getStockOnHand() + request.getAdjustment();
                if (adjustedStock < 0) {
                    return Mono.error(new BusinessRuleException(
                        ApiErrorCodes.NEGATIVE_STOCK_NOT_ALLOWED,
                        "The stock adjustment would produce a negative quantity."));
                }
                if (adjustedStock > Integer.MAX_VALUE) {
                    return Mono.error(new BusinessRuleException(
                        ApiErrorCodes.INVALID_INGREDIENT_CATALOG,
                        "The stock adjustment exceeds the supported range."));
                }
                ingredient.setStockOnHand((int) adjustedStock);
                if (adjustedStock == 0) {
                    ingredient.setAvailable(false);
                }
                normalizeAndValidate(ingredient);
                return ingredientRepo.save(ingredient);
            });
    }

    private Mono<Ingredient> findIngredient(String ingredientId) {
        return ingredientRepo.findById(ingredientId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                ApiErrorCodes.INGREDIENT_NOT_FOUND, "Ingredient was not found.")));
    }

    private void normalizeAndValidate(Ingredient ingredient) {
        BigDecimal unitPrice = ingredient.getUnitPrice();
        if (unitPrice == null || unitPrice.signum() < 0 || ingredient.getReorderLevel() < 0) {
            throw new BusinessRuleException(ApiErrorCodes.INVALID_INGREDIENT_CATALOG,
                "Ingredient catalog values are invalid.");
        }
        if (ingredient.getStockOnHand() < 0) {
            throw new BusinessRuleException(ApiErrorCodes.NEGATIVE_STOCK_NOT_ALLOWED,
                "Ingredient stock cannot be negative.");
        }
        if (ingredient.isAvailable() && ingredient.getStockOnHand() == 0) {
            throw new BusinessRuleException(ApiErrorCodes.INVALID_INGREDIENT_CATALOG,
                "An ingredient without stock cannot be available for sale.");
        }
        ingredient.setUnitPrice(unitPrice.setScale(PRICE_SCALE, PRICE_ROUNDING));
    }
}
//Fin TC-13
