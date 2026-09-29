package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.api.dto.IngredientCatalogUpdateRequest;
import tacos.api.dto.IngredientRequest;
import tacos.api.dto.StockAdjustmentRequest;
import tacos.api.error.BusinessRuleException;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.IngredientMapper;
import tacos.data.IngredientRepository;

//TC-13 - Reglas reactivas de catalogo e inventario
public class IngredientCatalogServiceTest {
    private IngredientRepository ingredientRepo;
    private IngredientCatalogService ingredientCatalogService;

    @BeforeEach
    public void setUp() {
        ingredientRepo = Mockito.mock(IngredientRepository.class);
        ingredientCatalogService = new IngredientCatalogService(ingredientRepo, new IngredientMapper());
        when(ingredientRepo.save(any(Ingredient.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    @Test
    public void shouldStoreUnitPriceUsingBigDecimalAndConfiguredScale() {
        IngredientRequest request = ingredientRequest(new BigDecimal("1.105"), true, 10, 2);

        StepVerifier.create(ingredientCatalogService.create(request))
            .assertNext(ingredient -> {
                assertThat(ingredient.getUnitPrice()).isEqualByComparingTo("1.11");
                assertThat(ingredient.getUnitPrice().scale()).isEqualTo(2);
            })
            .verifyComplete();
    }

    @Test
    public void shouldUpdateCatalogFieldsWithoutChangingIdentityStockOrVersion() {
        Ingredient ingredient = ingredient(10, true);
        ingredient.setVersion(4L);
        IngredientCatalogUpdateRequest request = new IngredientCatalogUpdateRequest();
        request.setUnitPrice(new BigDecimal("2.355"));
        request.setAvailable(false);
        request.setReorderLevel(3);
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));

        StepVerifier.create(ingredientCatalogService.updateCatalog("FLTO", request))
            .assertNext(updated -> {
                assertThat(updated.getId()).isEqualTo("FLTO");
                assertThat(updated.getUnitPrice()).isEqualByComparingTo("2.36");
                assertThat(updated.isAvailable()).isFalse();
                assertThat(updated.getStockOnHand()).isEqualTo(10);
                assertThat(updated.getReorderLevel()).isEqualTo(3);
                assertThat(updated.getVersion()).isEqualTo(4L);
            })
            .verifyComplete();

        verify(ingredientRepo).save(ingredient);
    }

    @Test
    public void shouldIncreaseStock() {
        Ingredient ingredient = ingredient(10, true);
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));

        StepVerifier.create(ingredientCatalogService.adjustStock("FLTO", adjustment(5)))
            .assertNext(updated -> assertThat(updated.getStockOnHand()).isEqualTo(15))
            .verifyComplete();
    }

    @Test
    public void shouldDecreaseStockWithoutGoingNegative() {
        Ingredient ingredient = ingredient(10, true);
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));

        StepVerifier.create(ingredientCatalogService.adjustStock("FLTO", adjustment(-4)))
            .assertNext(updated -> assertThat(updated.getStockOnHand()).isEqualTo(6))
            .verifyComplete();
    }

    @Test
    public void shouldDisableIngredientWhenStockReachesZero() {
        Ingredient ingredient = ingredient(5, true);
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));

        StepVerifier.create(ingredientCatalogService.adjustStock("FLTO", adjustment(-5)))
            .assertNext(updated -> {
                assertThat(updated.getStockOnHand()).isZero();
                assertThat(updated.isAvailable()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    public void shouldRejectAdjustmentThatWouldProduceNegativeStock() {
        Ingredient ingredient = ingredient(5, true);
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));

        StepVerifier.create(ingredientCatalogService.adjustStock("FLTO", adjustment(-6)))
            .expectErrorMatches(error -> error instanceof BusinessRuleException
                && "NEGATIVE_STOCK_NOT_ALLOWED".equals(((BusinessRuleException) error).getCode()))
            .verify();

        verify(ingredientRepo, never()).save(any(Ingredient.class));
    }

    @Test
    public void shouldRejectAvailableIngredientWithoutStock() {
        Ingredient ingredient = ingredient(0, false);
        IngredientCatalogUpdateRequest request = new IngredientCatalogUpdateRequest();
        request.setAvailable(true);
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));

        StepVerifier.create(ingredientCatalogService.updateCatalog("FLTO", request))
            .expectError(BusinessRuleException.class)
            .verify();

        verify(ingredientRepo, never()).save(any(Ingredient.class));
    }

    @Test
    public void shouldRejectNegativeCatalogValuesBeforeSave() {
        Ingredient ingredient = ingredient(10, true);
        IngredientCatalogUpdateRequest request = new IngredientCatalogUpdateRequest();
        request.setUnitPrice(new BigDecimal("-0.01"));
        request.setReorderLevel(-1);
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));

        StepVerifier.create(ingredientCatalogService.updateCatalog("FLTO", request))
            .expectErrorMatches(error -> error instanceof BusinessRuleException
                && "INVALID_INGREDIENT_CATALOG".equals(((BusinessRuleException) error).getCode()))
            .verify();

        verify(ingredientRepo, never()).save(any(Ingredient.class));
    }

    @Test
    public void shouldAllowCommercialPauseWithPositiveStock() {
        Ingredient ingredient = ingredient(10, true);
        IngredientCatalogUpdateRequest request = new IngredientCatalogUpdateRequest();
        request.setAvailable(false);
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));

        StepVerifier.create(ingredientCatalogService.updateCatalog("FLTO", request))
            .assertNext(updated -> {
                assertThat(updated.isAvailable()).isFalse();
                assertThat(updated.getStockOnHand()).isEqualTo(10);
            })
            .verifyComplete();
    }

    @Test
    public void shouldReturnNotFoundWithoutCreatingIngredient() {
        when(ingredientRepo.findById("TC13-NOT-FOUND")).thenReturn(Mono.empty());

        StepVerifier.create(ingredientCatalogService.adjustStock(
                "TC13-NOT-FOUND", adjustment(1)))
            .expectError(ResourceNotFoundException.class)
            .verify();

        verify(ingredientRepo, never()).save(any(Ingredient.class));
    }

    private Ingredient ingredient(int stock, boolean available) {
        return new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP,
            new BigDecimal("1.25"), available, stock, 2);
    }

    private IngredientRequest ingredientRequest(BigDecimal price, boolean available,
            int stock, int reorderLevel) {
        IngredientRequest request = new IngredientRequest();
        request.setId("FLTO");
        request.setName("Flour Tortilla");
        request.setType(Ingredient.Type.WRAP);
        request.setUnitPrice(price);
        request.setAvailable(available);
        request.setStockOnHand(stock);
        request.setReorderLevel(reorderLevel);
        return request;
    }

    private StockAdjustmentRequest adjustment(int quantity) {
        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setAdjustment(quantity);
        return request;
    }
}
//Fin TC-13
