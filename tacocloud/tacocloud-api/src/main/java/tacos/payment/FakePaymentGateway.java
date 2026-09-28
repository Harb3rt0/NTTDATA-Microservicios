package tacos.payment;

import java.util.UUID;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

//TC-12 - Adaptador fake que genera un token opaco para el laboratorio
@Component
public class FakePaymentGateway implements PaymentGateway {

    @Override
    public Mono<TokenizedPayment> tokenize(PaymentTokenizationCommand command) {
        return Mono.fromSupplier(() -> {
            String cardNumber = command.getCardNumber();
            String last4 = cardNumber.substring(cardNumber.length() - 4);
            String token = "labtok_" + UUID.randomUUID().toString().replace("-", "")
                .replace('0', 'g').replace('1', 'h').replace('2', 'i').replace('3', 'j')
                .replace('4', 'k').replace('5', 'l').replace('6', 'm').replace('7', 'n')
                .replace('8', 'o').replace('9', 'p'); //modificacion para TC-12
            return new TokenizedPayment(token, "LAB_CARD", last4, command.getExpiration());
        });
    }
}
//Fin TC-12
