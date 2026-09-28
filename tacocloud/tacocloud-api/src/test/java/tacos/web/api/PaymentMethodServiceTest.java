package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.PaymentMethod;
import tacos.User;
import tacos.api.dto.PaymentMethodTokenizeRequest;
import tacos.data.PaymentMethodRepository;
import tacos.data.UserRepository;
import tacos.payment.PaymentGateway;
import tacos.payment.PaymentTokenizationCommand;
import tacos.payment.TokenizedPayment;

//TC-12 - Persistencia reactiva segura de metodos de pago
public class PaymentMethodServiceTest {
    private PaymentGateway paymentGateway;
    private PaymentMethodRepository paymentMethodRepo;
    private UserRepository userRepo;
    private PaymentMethodService paymentMethodService;

    @BeforeEach
    public void setUp() {
        paymentGateway = Mockito.mock(PaymentGateway.class);
        paymentMethodRepo = Mockito.mock(PaymentMethodRepository.class);
        userRepo = Mockito.mock(UserRepository.class);
        paymentMethodService = new PaymentMethodService(paymentGateway, paymentMethodRepo, userRepo);
    }

    @Test
    public void shouldEmitSafeResponseOnlyAfterSavingTokenizedPayment() {
        User user = user("alice");
        PaymentMethodTokenizeRequest request = request();
        AtomicBoolean saveSubscribed = new AtomicBoolean(false);
        when(userRepo.findByUsername("alice")).thenReturn(Mono.just(user));
        when(paymentGateway.tokenize(any(PaymentTokenizationCommand.class)))
            .thenReturn(Mono.just(new TokenizedPayment(
                "labtok_opaque_value", "LAB_CARD", "9999", "12/39")));
        when(paymentMethodRepo.save(any(PaymentMethod.class))).thenAnswer(invocation -> Mono.defer(() -> {
            saveSubscribed.set(true);
            PaymentMethod paymentMethod = invocation.getArgument(0);
            paymentMethod.setId("PAYMENT1");
            return Mono.just(paymentMethod);
        }));

        StepVerifier.create(paymentMethodService.tokenize(request, authentication("alice")))
            .assertNext(response -> {
                assertThat(saveSubscribed).isTrue();
                assertThat(response.getId()).isEqualTo("PAYMENT1");
                assertThat(response.getBrand()).isEqualTo("LAB_CARD");
                assertThat(response.getLast4()).isEqualTo("9999");
            })
            .verifyComplete();

        ArgumentCaptor<PaymentMethod> paymentCaptor = ArgumentCaptor.forClass(PaymentMethod.class);
        verify(paymentMethodRepo, times(1)).save(paymentCaptor.capture());
        PaymentMethod stored = paymentCaptor.getValue();
        assertThat(stored.getPaymentToken()).isEqualTo("labtok_opaque_value");
        assertThat(stored.getPaymentToken()).doesNotContain(request.getCardNumber());
        assertThat(stored.getLast4()).isEqualTo("9999");
        verify(paymentGateway, times(1)).tokenize(any(PaymentTokenizationCommand.class));
    }

    private PaymentMethodTokenizeRequest request() {
        PaymentMethodTokenizeRequest request = new PaymentMethodTokenizeRequest();
        request.setCardNumber("9999999999999999");
        request.setExpiration("12/39");
        request.setCvv("999");
        return request;
    }

    private Authentication authentication(String username) {
        return new UsernamePasswordAuthenticationToken(username, "n/a",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private User user(String username) {
        User user = new User(username, "{bcrypt}hash", "Test User", "Street", "City",
            "TX", "78701", "555-0100", username + "@example.com");
        user.setId("USER1");
        return user;
    }
}
//Fin TC-12
