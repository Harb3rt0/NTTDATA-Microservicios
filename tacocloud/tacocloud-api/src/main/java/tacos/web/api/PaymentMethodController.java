package tacos.web.api;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.api.dto.PaymentMethodResponse;
import tacos.api.dto.PaymentMethodTokenizeRequest;

//TC-12 - Endpoint autenticado de tokenizacion simulada
@RestController
@RequestMapping(path = "/api/payment-methods", produces = "application/json")
public class PaymentMethodController {
    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @PostMapping(path = "/tokenize", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PaymentMethodResponse> tokenize(@Valid @RequestBody PaymentMethodTokenizeRequest request,
            Authentication authentication) {
        return paymentMethodService.tokenize(request, authentication);
    }
}
//Fin TC-12
