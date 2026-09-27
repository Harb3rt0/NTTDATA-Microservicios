package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.TacoOrder;
import tacos.api.mapper.IngredientMapper;
import tacos.api.mapper.OrderMapper;
import tacos.api.mapper.TacoMapper;
import tacos.data.IngredientRepository;
import tacos.data.OrderRepository;
import tacos.messaging.OrderMessagingService;

public class OrderServiceTest {
    private OrderRepository orderRepo;
    private IngredientRepository ingredientRepo;
    private OrderMessagingService orderMessages;
    private IngredientMapper ingredientMapper;
    private TacoMapper tacoMapper;
    private OrderMapper orderMapper;
    private OrderService orderService;

    @BeforeEach
    public void setUp() {
        orderRepo = Mockito.mock(OrderRepository.class);
        ingredientRepo = Mockito.mock(IngredientRepository.class);
        orderMessages = Mockito.mock(OrderMessagingService.class);
        ingredientMapper = new IngredientMapper();
        tacoMapper = new TacoMapper(ingredientMapper);
        orderMapper = new OrderMapper(tacoMapper);
        
        orderService = new OrderService(orderRepo, ingredientRepo, orderMessages, tacoMapper, orderMapper);
    }

    //pruebas TC-07
    @Test
    public void shouldSaveAndPublishExactlyOnce() {
        TacoOrder order = new TacoOrder();
        order.setId("ORDER1");

        when(orderRepo.save(order)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.saveAndPublish(order))
            .assertNext(savedOrder -> assertEquals("ORDER1",savedOrder.getId()))
            .verifyComplete();

        verify(orderRepo, times(1)).save(order);
        verify(orderMessages, times(1)).sendOrder(order);
    }

    @Test
    public void shouldNotPublishWhenSaveFails() {
        TacoOrder order = new TacoOrder();
        order.setId("ORDER1");

        RuntimeException saveError = new RuntimeException("Database error");

        when(orderRepo.save(order)).thenReturn(Mono.error(saveError));

        StepVerifier.create(orderService.saveAndPublish(order))
            .expectErrorMatches(error -> error instanceof RuntimeException && error.getMessage().equals("Database error"))
            .verify();

        verify(orderRepo, times(1)).save(order);
        verify(orderMessages, never()).sendOrder(order);
    }
    //final de pruebas TC-07
}
