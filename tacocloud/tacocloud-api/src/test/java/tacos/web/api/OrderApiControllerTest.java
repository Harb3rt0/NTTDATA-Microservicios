package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.TacoOrder;
import tacos.api.dto.OrderPatchRequest;
import tacos.api.dto.OrderResponse;
import tacos.api.dto.OrderUpdateRequest;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.IngredientMapper;
import tacos.api.mapper.OrderMapper;
import tacos.api.mapper.TacoMapper;
import tacos.data.OrderRepository;
import tacos.messaging.OrderMessagingService;

public class OrderApiControllerTest {

    //pruebas TC-04
    @Test
    public void shouldPatchOrderWithoutChangingUnallowedFields() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);   //cambio en TC-07
        IngredientMapper ingredientMapper = new IngredientMapper(); //cambio en TC-08
        TacoMapper tacoMapper = new TacoMapper(ingredientMapper);
        OrderMapper orderMapper = new OrderMapper(tacoMapper);
        
        TacoOrder existingOrder = new TacoOrder();
        existingOrder.setId("ORDER1");
        existingOrder.setDeliveryState("CA");
        existingOrder.setDeliveryZip("90210");
        existingOrder.setPaymentMethodId("PAYMENT1"); //modificacion para TC-12

        when(orderService.patchOrder(Mockito.eq("ORDER1"), any(OrderPatchRequest.class),
            nullable(Authentication.class))).thenAnswer(invocation -> {
                orderMapper.patchEntity(invocation.getArgument(1), existingOrder);
                return Mono.just(existingOrder);
            });

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService, orderMapper)
        ).build();

        testClient.patch().uri("/api/orders/ORDER1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"deliveryZip\":\"12345\", \"paymentMethodId\":\"OTHER_PAYMENT\"}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.deliveryZip").isEqualTo("12345")
            .jsonPath("$.deliveryState").isEqualTo("CA")
            .jsonPath("$.paymentMethodId").doesNotExist();

        assertEquals("PAYMENT1", existingOrder.getPaymentMethodId()); //modificacion para TC-12
    }

    @Test
    public void shouldReturnNotFoundWhenPatchingNonExistingOrder() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);   //cambio en TC-07
        IngredientMapper ingredientMapper = new IngredientMapper(); //cambio en TC-08
        TacoMapper tacoMapper = new TacoMapper(ingredientMapper);
        OrderMapper orderMapper = new OrderMapper(tacoMapper);

        when(orderService.patchOrder(Mockito.eq("NON_EXISTING"), any(OrderPatchRequest.class),
            nullable(Authentication.class))).thenReturn(Mono.error(new ResourceNotFoundException(
                ApiErrorCodes.ORDER_NOT_FOUND, "Order was not found.")));

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService, orderMapper)
        ).build();

        OrderPatchRequest patchRequest = new OrderPatchRequest();
        patchRequest.setDeliveryZip("12345");

        testClient.patch().uri("/api/orders/NON_EXISTING")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(patchRequest)
            .exchange()
            .expectStatus().isNotFound();
    }
    //final de pruebas TC-04

    //pruebas TC-05
    @Test
    public void shouldNotAllowBodyIdToRedirectPutOrder() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);   //cambio en TC-07
        IngredientMapper ingredientMapper = new IngredientMapper(); //cambio en TC-08
        TacoMapper tacoMapper = new TacoMapper(ingredientMapper);
        OrderMapper orderMapper = new OrderMapper(tacoMapper);

        TacoOrder existingOrder = new TacoOrder();
        existingOrder.setId("ORDER1");
        existingOrder.setDeliveryName("Original Name");
        existingOrder.setDeliveryStreet("Original Street");
        existingOrder.setDeliveryCity("Original City");
        existingOrder.setDeliveryState("AG");
        existingOrder.setDeliveryZip("20000");

        TacoOrder updatedOrder = new TacoOrder();
        updatedOrder.setId("ORDER1");
        updatedOrder.setDeliveryName("Updated Name");
        updatedOrder.setDeliveryStreet("Updated Street");
        updatedOrder.setDeliveryCity("Updated City");
        updatedOrder.setDeliveryState("JC");
        updatedOrder.setDeliveryZip("44100");

        when(orderService.updateOrder(Mockito.eq("ORDER1"), any(OrderUpdateRequest.class),
            nullable(Authentication.class)))
            .thenReturn(Mono.just(updatedOrder));

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService, orderMapper)
        ).build();

        testClient.put().uri("/api/orders/ORDER1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"id\":\"OTHER_ORDER\",\"deliveryName\":\"Updated Name\","
                + "\"deliveryStreet\":\"Updated Street\",\"deliveryCity\":\"Updated City\","
                + "\"deliveryState\":\"JC\",\"deliveryZip\":\"44100\","
                + "\"tacos\":[{\"name\":\"Test taco\",\"ingredientIds\":[\"FLTO\"]}]}")
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderResponse.class)
            .value(responseOrder -> {
                assertEquals("ORDER1", responseOrder.getId());
                assertEquals("Updated Name", responseOrder.getDeliveryName());
                assertEquals("44100", responseOrder.getDeliveryZip());
            });

        verify(orderService).updateOrder(Mockito.eq("ORDER1"), any(OrderUpdateRequest.class),
            nullable(Authentication.class));
        verify(orderService, never()).updateOrder(Mockito.eq("OTHER_ORDER"),
            any(OrderUpdateRequest.class), nullable(Authentication.class));
    }
    
    @Test
    public void shouldDeleteExistingOrder() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);   //cambio en TC-07
        IngredientMapper ingredientMapper = new IngredientMapper(); //cambio en TC-08
        TacoMapper tacoMapper = new TacoMapper(ingredientMapper);
        OrderMapper orderMapper = new OrderMapper(tacoMapper);

        TacoOrder existingOrder = new TacoOrder();
        existingOrder.setId("ORDER1");

        when(orderService.deleteOrder(Mockito.eq("ORDER1"), nullable(Authentication.class)))
            .thenReturn(Mono.empty());

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService, orderMapper)
        ).build();

        testClient.delete().uri("/api/orders/ORDER1")
            .exchange()
            .expectStatus().isNoContent()
            .expectBody()
            .isEmpty();

        verify(orderService, times(1)).deleteOrder(Mockito.eq("ORDER1"),
            nullable(Authentication.class));
    }

    @Test
    public void shouldReturnNotFoundWhenDeletingNonExistingOrder() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);   //cambio en TC-07
        IngredientMapper ingredientMapper = new IngredientMapper(); //cambio en TC-08
        TacoMapper tacoMapper = new TacoMapper(ingredientMapper);
        OrderMapper orderMapper = new OrderMapper(tacoMapper);

        when(orderService.deleteOrder(Mockito.eq("NON_EXISTING"), nullable(Authentication.class)))
            .thenReturn(Mono.error(new ResourceNotFoundException(
                ApiErrorCodes.ORDER_NOT_FOUND, "Order was not found.")));

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService, orderMapper)
        ).build();

        testClient.delete().uri("/api/orders/NON_EXISTING")
            .exchange()
            .expectStatus().isNotFound();

        verify(orderService).deleteOrder(Mockito.eq("NON_EXISTING"), nullable(Authentication.class));
    }
    //final de pruebas TC-05

    //pruebas TC-07
    @Test
    public void shouldNotSaveOrPublishWhenEmailConversionFails() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);
        IngredientMapper ingredientMapper = new IngredientMapper(); //cambio en TC-08
        TacoMapper tacoMapper = new TacoMapper(ingredientMapper);
        OrderMapper orderMapper = new OrderMapper(tacoMapper);

        EmailOrder emailOrder = new EmailOrder();
        emailOrder.setEmail("invalid@gmail.com");

        when(emailOrderService.convertEmailOrderToDomainOrder(any()))
            .thenReturn(Mono.error(new IllegalArgumentException("User not found")));

        OrderApiController controller = new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService, orderMapper);

        StepVerifier.create(controller.postOrderFromEmail(emailOrder))
            .expectErrorMatches(error ->
                error instanceof IllegalArgumentException && error.getMessage()
                    .equals("User not found")
            )
            .verify();

        verify(orderService, never()).saveAndPublish(any(TacoOrder.class));
    }

    @Test
    public void shouldConvertSaveAndPublishOrderOnce() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);
        IngredientMapper ingredientMapper = new IngredientMapper(); //cambio en TC-08
        TacoMapper tacoMapper = new TacoMapper(ingredientMapper);
        OrderMapper orderMapper = new OrderMapper(tacoMapper);

        EmailOrder emailOrder = new EmailOrder();
        emailOrder.setEmail("test.taco@gmail.com");

        TacoOrder convertedOrder = new TacoOrder();

        convertedOrder.setId("ORDER1");

        when(emailOrderService.convertEmailOrderToDomainOrder(any())).thenReturn(Mono.just(convertedOrder));
        when(orderService.saveAndPublish(convertedOrder)).thenReturn(Mono.just(convertedOrder));

        OrderApiController controller = new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService, orderMapper);

        StepVerifier.create(controller.postOrderFromEmail(emailOrder))
            .assertNext(order -> {
                assertEquals("ORDER1", order.getId());
            })
            .verifyComplete();

        verify(emailOrderService, times(1)).convertEmailOrderToDomainOrder(any());
        verify(orderService, times(1)).saveAndPublish(convertedOrder);
    }

    @Test
    public void shouldSubscribeToConversionOnlyOnce() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);
        IngredientMapper ingredientMapper = new IngredientMapper(); //cambio en TC-08
        TacoMapper tacoMapper = new TacoMapper(ingredientMapper);
        OrderMapper orderMapper = new OrderMapper(tacoMapper);

        AtomicInteger subscriptions = new AtomicInteger(0);
        
        EmailOrder emailOrder = new EmailOrder();
        TacoOrder convertedOrder = new TacoOrder();

        convertedOrder.setId("ORDER1");
        convertedOrder.setDeliveryName("Test User");

        Mono<TacoOrder> coldPublisher = Mono.defer(() -> {
            subscriptions.incrementAndGet();
            return Mono.just(convertedOrder);
        });

        when(emailOrderService.convertEmailOrderToDomainOrder(any())).thenReturn(coldPublisher);
        when(orderService.saveAndPublish(convertedOrder)).thenReturn(Mono.just(convertedOrder));

        OrderApiController controller = new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService, orderMapper);

        StepVerifier.create(controller.postOrderFromEmail(emailOrder)).assertNext(response -> {
                assertEquals("ORDER1", response.getId());
                assertEquals("Test User", response.getDeliveryName());
            }
        ).verifyComplete();

        assertEquals(1, subscriptions.get());
    }

    //final de pruebas TC-07
}
