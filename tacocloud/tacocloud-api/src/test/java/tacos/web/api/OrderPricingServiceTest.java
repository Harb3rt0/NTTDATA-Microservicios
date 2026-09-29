package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.OrderLine;
import tacos.api.dto.OrderLineCreateRequest;
import tacos.api.dto.TacoCreateRequest;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;
import tacos.api.mapper.IngredientMapper;
import tacos.api.mapper.TacoMapper;
import tacos.data.IngredientRepository;

//TC-14 - Pruebas unitarias de precios y cantidades
public class OrderPricingServiceTest {
    private IngredientRepository ingredientRepo;
    private OrderPricingService pricingService;

    @BeforeEach
    public void setUp() {
        ingredientRepo = Mockito.mock(IngredientRepository.class);
        pricingService = new OrderPricingService(ingredientRepo,
            new TacoMapper(new IngredientMapper()), 10, "USD");
    }

    @Test
    public void shouldCalculateDecimalSubtotalWithoutFloatingPointErrors() {
        when(ingredientRepo.findById("A")).thenReturn(Mono.just(ingredient("A", "1.25")));
        when(ingredientRepo.findById("B")).thenReturn(Mono.just(ingredient("B", "2.30")));

        StepVerifier.create(pricingService.priceItem(item(2, "A", "B")))
            .assertNext(line -> {
                assertEquals(new BigDecimal("3.55"), line.getUnitPriceAtPurchase());
                assertEquals(new BigDecimal("7.10"), line.getSubtotal());
                assertEquals(2, line.getQuantity());
                assertEquals(2, line.getSubtotal().scale());
            })
            .verifyComplete();
    }

    @Test
    public void shouldDoubleSubtotalForQuantityTwo() {
        when(ingredientRepo.findById("A")).thenReturn(Mono.just(ingredient("A", "1.25")));
        when(ingredientRepo.findById("B")).thenReturn(Mono.just(ingredient("B", "2.30")));

        StepVerifier.create(Flux.concat(
                pricingService.priceItem(item(1, "A", "B")),
                pricingService.priceItem(item(2, "A", "B"))).collectList())
            .assertNext(lines -> {
                assertEquals(new BigDecimal("3.55"), lines.get(0).getSubtotal());
                assertEquals(new BigDecimal("7.10"), lines.get(1).getSubtotal());
            })
            .verifyComplete();
    }

    @Test
    public void shouldRejectQuantityAboveConfiguredMaximumBeforeIngredientLookup() {
        StepVerifier.create(pricingService.priceItems(Arrays.asList(item(11, "A"))))
            .expectErrorMatches(error -> error instanceof BusinessRuleException
                && ApiErrorCodes.ITEM_QUANTITY_LIMIT_EXCEEDED.equals(
                    ((BusinessRuleException) error).getCode()))
            .verify();

        verify(ingredientRepo, never()).findById("A");
    }

    @Test
    public void shouldRejectNonPositiveQuantityBeforeIngredientLookup() {
        StepVerifier.create(pricingService.priceItems(Arrays.asList(item(0, "A"))))
            .expectErrorMatches(error -> error instanceof BusinessRuleException
                && ApiErrorCodes.INVALID_ITEM_QUANTITY.equals(
                    ((BusinessRuleException) error).getCode()))
            .verify();

        verify(ingredientRepo, never()).findById("A");
    }

    @Test
    public void shouldRejectNegativeQuantityBeforeIngredientLookup() {
        StepVerifier.create(pricingService.priceItems(Arrays.asList(item(-1, "A"))))
            .expectErrorMatches(error -> error instanceof BusinessRuleException
                && ApiErrorCodes.INVALID_ITEM_QUANTITY.equals(
                    ((BusinessRuleException) error).getCode()))
            .verify();

        verify(ingredientRepo, never()).findById("A");
    }

    @Test
    public void shouldCalculateOrderTotalFromPersistableSnapshots() {
        OrderLine first = new OrderLine();
        first.setSubtotal(new BigDecimal("1.10"));
        OrderLine second = new OrderLine();
        second.setSubtotal(new BigDecimal("2.20"));

        assertEquals(new BigDecimal("3.30"),
            pricingService.calculateTotal(Arrays.asList(first, second)));
        assertEquals("USD", pricingService.getCurrency());
    }

    private OrderLineCreateRequest item(int quantity, String... ingredientIds) {
        TacoCreateRequest taco = new TacoCreateRequest();
        taco.setName("Test taco");
        taco.setIngredientIds(Arrays.asList(ingredientIds));
        OrderLineCreateRequest item = new OrderLineCreateRequest();
        item.setTaco(taco);
        item.setQuantity(quantity);
        return item;
    }

    private Ingredient ingredient(String id, String price) {
        return new Ingredient(id, id, Ingredient.Type.PROTEIN, new BigDecimal(price), true, 10, 2);
    }
}
//Fin TC-14
