package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.TacoOrder;
import tacos.data.OrderRepository;
import tacos.messaging.OrderMessagingService;
import tacos.web.api.dto.OrderPatchRequest;

public class OrderApiControllerTest {

    //pruebas TC-04
    @Test
    public void shouldPatchOrderWithoutChangingUnallowedFields() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);   //cambio en TC-07
        
        TacoOrder existingOrder = new TacoOrder();
        existingOrder.setId("ORDER1");
        existingOrder.setDeliveryState("CA");
        existingOrder.setDeliveryZip("90210");
        existingOrder.setCcNumber("1111222233334444");

        when(orderRepo.findById("ORDER1")).thenReturn(Mono.just(existingOrder));
        when(orderRepo.save(any(TacoOrder.class))).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService)
        ).build();

        testClient.patch().uri("/api/orders/ORDER1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"deliveryZip\":\"12345\", \"ccNumber\":\"9999999999999999\"}")
            .exchange()
            .expectStatus().isOk()
            .expectBody(TacoOrder.class)
            .value(responseOrder -> {
                assert responseOrder.getDeliveryZip().equals("12345");
                assert responseOrder.getDeliveryState().equals("CA");
                assert responseOrder.getCcNumber().equals("1111222233334444");
            });
    }

    @Test
    public void shouldReturnNotFoundWhenPatchingNonExistingOrder() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);   //cambio en TC-07

        when(orderRepo.findById("NON_EXISTING")).thenReturn(Mono.empty());

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService)
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
        TacoOrder existingOrder = new TacoOrder();
        existingOrder.setId("ORDER1");
        existingOrder.setDeliveryName("Original Name");
        existingOrder.setDeliveryStreet("Original Street");
        existingOrder.setDeliveryCity("Original City");
        existingOrder.setDeliveryState("AG");
        existingOrder.setDeliveryZip("20000");

        TacoOrder requestOrder = new TacoOrder();
        requestOrder.setId("OTHER_ORDER");
        requestOrder.setDeliveryName("Updated Name");
        requestOrder.setDeliveryStreet("Updated Street");
        requestOrder.setDeliveryCity("Updated City");
        requestOrder.setDeliveryState("JC");
        requestOrder.setDeliveryZip("44100");

        when(orderRepo.findById("ORDER1")).thenReturn(Mono.just(existingOrder));
        when(orderRepo.save(any(TacoOrder.class))).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService)
        ).build();

        testClient.put().uri("/api/orders/ORDER1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestOrder)
            .exchange()
            .expectStatus().isOk()
            .expectBody(TacoOrder.class)
            .value(responseOrder -> {
                assertEquals("ORDER1", responseOrder.getId());
                assertEquals("Updated Name", responseOrder.getDeliveryName());
                assertEquals("44100", responseOrder.getDeliveryZip());
            });

        verify(orderRepo).findById("ORDER1");
        verify(orderRepo).save(any(TacoOrder.class));
        verify(orderRepo, never()).findById("OTHER_ORDER");
    }
    
    @Test
    public void shouldDeleteExistingOrder() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);   //cambio en TC-07

        TacoOrder existingOrder = new TacoOrder();
        existingOrder.setId("ORDER1");

        when(orderRepo.findById("ORDER1")).thenReturn(Mono.just(existingOrder));
        when(orderRepo.deleteById("ORDER1")).thenReturn(Mono.empty());

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService)
        ).build();

        testClient.delete().uri("/api/orders/ORDER1")
            .exchange()
            .expectStatus().isNoContent()
            .expectBody()
            .isEmpty();

        verify(orderRepo).findById("ORDER1");
        verify(orderRepo, times(1)).deleteById("ORDER1");
    }

    @Test
    public void shouldReturnNotFoundWhenDeletingNonExistingOrder() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);   //cambio en TC-07

        when(orderRepo.findById("NON_EXISTING")).thenReturn(Mono.empty());

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService)
        ).build();

        testClient.delete().uri("/api/orders/NON_EXISTING")
            .exchange()
            .expectStatus().isNotFound();

        verify(orderRepo).findById("NON_EXISTING");
        verify(orderRepo, never()).deleteById(anyString());
    }
    //final de pruebas TC-05

    //pruebas TC-07
    @Test
    public void shouldNotSaveOrPublishWhenEmailConversionFails() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        OrderService orderService = Mockito.mock(OrderService.class);

        EmailOrder emailOrder = new EmailOrder();
        emailOrder.setEmail("invalid@gmail.com");

        when(emailOrderService.convertEmailOrderToDomainOrder(any()))
            .thenReturn(Mono.error(new IllegalArgumentException("User not found")));

        OrderApiController controller = new OrderApiController(orderRepo, orderMessages, emailOrderService, orderService);

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

        EmailOrder emailOrder = new EmailOrder();
        emailOrder.setEmail("test.taco@gmail.com");

        TacoOrder convertedOrder = new TacoOrder();

        convertedOrder.setId("ORDER1");

        when(emailOrderService.convertEmailOrderToDomainOrder(any())).thenReturn(Mono.just(convertedOrder));
        when(orderService.saveAndPublish(convertedOrder)).thenReturn(Mono.just(convertedOrder));

        OrderApiController controller =
            new OrderApiController(
                orderRepo,
                orderMessages,
                emailOrderService,
                orderService
            );

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

        AtomicInteger subscriptions = new AtomicInteger(0);
        
        EmailOrder emailOrder = new EmailOrder();
        TacoOrder convertedOrder = new TacoOrder();

        convertedOrder.setId("ORDER1");

        Mono<TacoOrder> coldPublisher = Mono.defer(() -> {
                subscriptions.incrementAndGet();
                return Mono.just(convertedOrder);
            });

        when(emailOrderService.convertEmailOrderToDomainOrder(any())).thenReturn(coldPublisher);
        when(orderService.saveAndPublish(convertedOrder)).thenReturn(Mono.just(convertedOrder));

        OrderApiController controller =
            new OrderApiController(
                orderRepo,
                orderMessages,
                emailOrderService,
                orderService
            );

        StepVerifier.create(controller.postOrderFromEmail(emailOrder)).expectNext(convertedOrder).verifyComplete();

        assertEquals(1, subscriptions.get());
    }

    //final de pruebas TC-07
}
