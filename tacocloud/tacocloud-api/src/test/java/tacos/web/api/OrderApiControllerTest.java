package tacos.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import tacos.TacoOrder;
import tacos.data.OrderRepository;
import tacos.messaging.OrderMessagingService;
import tacos.web.api.dto.OrderPatchRequest;

public class OrderApiControllerTest {

    @Test
    public void shouldPatchOrderWithoutChangingUnallowedFields() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        OrderMessagingService orderMessages = Mockito.mock(OrderMessagingService.class);
        EmailOrderService emailOrderService = Mockito.mock(EmailOrderService.class);
        
        TacoOrder existingOrder = new TacoOrder();
        existingOrder.setId("ORDER1");
        existingOrder.setDeliveryState("CA");
        existingOrder.setDeliveryZip("90210");
        existingOrder.setCcNumber("1111222233334444");

        when(orderRepo.findById("ORDER1")).thenReturn(Mono.just(existingOrder));
        when(orderRepo.save(any(TacoOrder.class))).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService)
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

        when(orderRepo.findById("NON_EXISTING")).thenReturn(Mono.empty());

        WebTestClient testClient = WebTestClient.bindToController(
            new OrderApiController(orderRepo, orderMessages, emailOrderService)
        ).build();

        OrderPatchRequest patchRequest = new OrderPatchRequest();
        patchRequest.setDeliveryZip("12345");

        testClient.patch().uri("/api/orders/NON_EXISTING")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(patchRequest)
            .exchange()
            .expectStatus().isNotFound();
    }
}
