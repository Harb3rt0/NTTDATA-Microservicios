package tacos.payment;

import reactor.core.publisher.Mono;

//TC-12 - Puerto reactivo para tokenizacion de pagos
public interface PaymentGateway {
    Mono<TokenizedPayment> tokenize(PaymentTokenizationCommand command);
}
//Fin TC-12
