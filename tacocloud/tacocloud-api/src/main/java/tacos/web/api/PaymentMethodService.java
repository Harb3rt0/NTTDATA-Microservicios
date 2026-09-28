package tacos.web.api;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.PaymentMethod;
import tacos.User;
import tacos.api.dto.PaymentMethodResponse;
import tacos.api.dto.PaymentMethodTokenizeRequest;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;
import tacos.data.PaymentMethodRepository;
import tacos.data.UserRepository;
import tacos.payment.PaymentGateway;
import tacos.payment.PaymentGatewayException;
import tacos.payment.PaymentTokenizationCommand;

//TC-12 - Caso de uso reactivo para tokenizar y guardar metodos de pago
@Service
public class PaymentMethodService {
    private final PaymentGateway paymentGateway;
    private final PaymentMethodRepository paymentMethodRepo;
    private final UserRepository userRepo;

    public PaymentMethodService(PaymentGateway paymentGateway, PaymentMethodRepository paymentMethodRepo,
            UserRepository userRepo) {
        this.paymentGateway = paymentGateway;
        this.paymentMethodRepo = paymentMethodRepo;
        this.userRepo = userRepo;
    }

    public Mono<PaymentMethodResponse> tokenize(PaymentMethodTokenizeRequest request,
            Authentication authentication) {
        return authenticatedUser(authentication)
            .flatMap(user -> paymentGateway.tokenize(new PaymentTokenizationCommand(
                    request.getCardNumber(), request.getExpiration(), request.getCvv()))
                .onErrorMap(PaymentGatewayException.class, error -> new BusinessRuleException(
                    ApiErrorCodes.PAYMENT_TOKENIZATION_FAILED,
                    "The payment method could not be tokenized."))
                .map(tokenized -> new PaymentMethod(user, tokenized.getPaymentToken(),
                    tokenized.getBrand(), tokenized.getLast4(), tokenized.getExpiration()))
                .flatMap(paymentMethodRepo::save)
                .map(this::toResponse));
    }

    private Mono<User> authenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.error(new AccessDeniedException("Authentication is required."));
        }
        return userRepo.findByUsername(authentication.getName())
            .switchIfEmpty(Mono.error(new AccessDeniedException(
                "Authenticated user is unavailable.")));
    }

    private PaymentMethodResponse toResponse(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(paymentMethod.getId(), paymentMethod.getBrand(),
            paymentMethod.getLast4()); //modificacion para TC-12
    }
}
//Fin TC-12
