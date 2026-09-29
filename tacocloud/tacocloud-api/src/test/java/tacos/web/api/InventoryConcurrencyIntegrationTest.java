package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;
import tacos.Ingredient;
import tacos.InventoryReservation;
import tacos.OrderLine;
import tacos.ReservationStatus;
import tacos.Taco;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;

//TC-16 - Pruebas reales de atomicidad, compensacion e idempotencia
@DataMongoTest
@Import(InventoryService.class)
public class InventoryConcurrencyIntegrationTest {
    @Autowired
    private ReactiveMongoTemplate template;

    @Autowired
    private InventoryService inventoryService;

    @Test
    public void shouldAllowOnlyOneOfTwoConcurrentBuyersWhenStockIsOne() {
        Ingredient ingredient = ingredient("CONCURRENT", 1);
        Mono<reactor.util.function.Tuple3<reactor.core.publisher.Signal<InventoryReservation>,
                reactor.core.publisher.Signal<InventoryReservation>, Ingredient>> scenario =
            template.insert(ingredient)
                .then(Mono.zip(
                    inventoryService.reserve("BUYER-A", lines(ingredient, 1)).materialize(),
                    inventoryService.reserve("BUYER-B", lines(ingredient, 1)).materialize()))
                .flatMap(result -> template.findById("CONCURRENT", Ingredient.class)
                    .map(saved -> Tuples.of(result.getT1(), result.getT2(), saved)));

        StepVerifier.create(scenario)
            .assertNext(result -> {
                long successes = Arrays.asList(result.getT1(), result.getT2()).stream()
                    .filter(reactor.core.publisher.Signal::isOnNext).count();
                long insufficient = Arrays.asList(result.getT1(), result.getT2()).stream()
                    .filter(reactor.core.publisher.Signal::isOnError)
                    .filter(signal -> signal.getThrowable() instanceof BusinessRuleException
                        && ApiErrorCodes.INSUFFICIENT_STOCK.equals(
                            ((BusinessRuleException) signal.getThrowable()).getCode()))
                    .count();
                assertEquals(1, successes);
                assertEquals(1, insufficient);
                assertEquals(0, result.getT3().getStockOnHand());
            })
            .verifyComplete();
    }

    @Test
    public void shouldCompensatePreviouslyReservedIngredientsWhenLaterIngredientFails() {
        Ingredient first = ingredient("COMP-A", 1);
        Ingredient second = ingredient("COMP-B", 0);

        Mono<Object> scenario = template.insert(first).then(template.insert(second))
            .then(inventoryService.reserve("COMPENSATE", lines(first, second, 1)).materialize())
            .flatMap(signal -> Mono.zip(template.findById("COMP-A", Ingredient.class),
                    template.findById("COMP-B", Ingredient.class),
                    template.findById("COMPENSATE", InventoryReservation.class))
                .map(state -> Tuples.of(signal, state)));

        StepVerifier.create(scenario)
            .assertNext(raw -> {
                @SuppressWarnings("unchecked")
                reactor.util.function.Tuple2<reactor.core.publisher.Signal<InventoryReservation>,
                    reactor.util.function.Tuple3<Ingredient, Ingredient, InventoryReservation>> result =
                    (reactor.util.function.Tuple2<reactor.core.publisher.Signal<InventoryReservation>,
                        reactor.util.function.Tuple3<Ingredient, Ingredient, InventoryReservation>>) raw;
                assertTrue(result.getT1().isOnError());
                assertEquals(1, result.getT2().getT1().getStockOnHand());
                assertEquals(0, result.getT2().getT2().getStockOnHand());
                assertEquals(ReservationStatus.RELEASED, result.getT2().getT3().getStatus());
            })
            .verifyComplete();
    }

    @Test
    public void shouldNotDeductTwiceWhenReservationIsRetried() {
        Ingredient ingredient = ingredient("RETRY", 5);

        StepVerifier.create(template.insert(ingredient)
                .then(inventoryService.reserve("SAME-KEY", lines(ingredient, 2)))
                .then(inventoryService.reserve("SAME-KEY", lines(ingredient, 2)))
                .then(template.findById("RETRY", Ingredient.class)))
            .assertNext(saved -> assertEquals(3, saved.getStockOnHand()))
            .verifyComplete();
    }

    @Test
    public void shouldReleaseReservationOnlyOnce() {
        Ingredient ingredient = ingredient("RELEASE", 5);

        StepVerifier.create(template.insert(ingredient)
                .then(inventoryService.reserve("RELEASE-KEY", lines(ingredient, 2)))
                .then(inventoryService.release("RELEASE-KEY"))
                .then(inventoryService.release("RELEASE-KEY"))
                .then(template.findById("RELEASE", Ingredient.class)))
            .assertNext(saved -> assertEquals(5, saved.getStockOnHand()))
            .verifyComplete();
    }

    @Test
    public void shouldReserveQuantityAcrossOrderLinesCorrectlyAndInStableOrder() {
        Ingredient a = ingredient("QTY-A", 10);
        Ingredient b = ingredient("QTY-B", 10);
        OrderLine first = line(Arrays.asList(b, a), 2);
        OrderLine second = line(Collections.singletonList(a), 3);

        StepVerifier.create(template.insert(a).then(template.insert(b))
                .then(inventoryService.reserve("QTY-KEY", Arrays.asList(first, second)))
                .flatMap(reservation -> Mono.zip(Mono.just(reservation),
                    template.findById("QTY-A", Ingredient.class),
                    template.findById("QTY-B", Ingredient.class))))
            .assertNext(result -> {
                assertEquals("QTY-A", result.getT1().getItems().get(0).getIngredientId());
                assertEquals(5, result.getT1().getItems().get(0).getQuantity());
                assertEquals(5, result.getT2().getStockOnHand());
                assertEquals(8, result.getT3().getStockOnHand());
            })
            .verifyComplete();
    }

    private Ingredient ingredient(String id, int stock) {
        return new Ingredient(id, id, Ingredient.Type.VEGGIES, BigDecimal.ONE,
            true, stock, 0);
    }

    private java.util.List<OrderLine> lines(Ingredient ingredient, int quantity) {
        return Collections.singletonList(line(Collections.singletonList(ingredient), quantity));
    }

    private java.util.List<OrderLine> lines(Ingredient first, Ingredient second, int quantity) {
        return Collections.singletonList(line(Arrays.asList(first, second), quantity));
    }

    private OrderLine line(java.util.List<Ingredient> ingredients, int quantity) {
        Taco taco = new Taco();
        taco.setName("Inventory taco");
        taco.setIngredients(ingredients);
        OrderLine line = new OrderLine();
        line.setTaco(taco);
        line.setQuantity(quantity);
        return line;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
//Fin TC-16
