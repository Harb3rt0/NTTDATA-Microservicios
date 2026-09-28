package tacos.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import tacos.api.dto.PaymentMethodResponse;
import tacos.api.dto.PaymentMethodTokenizeRequest;

//TC-12 - Contrato HTTP seguro del endpoint de tokenizacion
public class PaymentMethodControllerTest {
    private PaymentMethodService paymentMethodService;
    private WebTestClient testClient;

    @BeforeEach
    public void setUp() {
        paymentMethodService = Mockito.mock(PaymentMethodService.class);
        testClient = WebTestClient.bindToController(new PaymentMethodController(paymentMethodService))
            .build();
    }

    @Test
    public void shouldReturnOnlySafePaymentFieldsAfterPersistence() {
        when(paymentMethodService.tokenize(any(PaymentMethodTokenizeRequest.class),
                nullable(Authentication.class)))
            .thenReturn(Mono.just(new PaymentMethodResponse(
                "PAYMENT1", "LAB_CARD", "9999")));

        testClient.post().uri("/api/payment-methods/tokenize")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"cardNumber\":\"9999999999999999\","
                + "\"expiration\":\"12/39\",\"cvv\":\"999\"}")
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.id").isEqualTo("PAYMENT1")
            .jsonPath("$.brand").isEqualTo("LAB_CARD")
            .jsonPath("$.last4").isEqualTo("9999")
            .jsonPath("$.paymentToken").doesNotExist()
            .jsonPath("$.cardNumber").doesNotExist()
            .jsonPath("$.cvv").doesNotExist()
            .jsonPath("$.user").doesNotExist();
    }

    @Test
    public void shouldValidateBeforeCallingServiceAndNotEchoInvalidValues() {
        testClient.post().uri("/api/payment-methods/tokenize")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"cardNumber\":\"INVALID_LAB_VALUE\","
                + "\"expiration\":\"99/99\",\"cvv\":\"X\"}")
            .exchange()
            .expectStatus().isBadRequest();

        verifyNoInteractions(paymentMethodService);
    }
}
//Fin TC-12
