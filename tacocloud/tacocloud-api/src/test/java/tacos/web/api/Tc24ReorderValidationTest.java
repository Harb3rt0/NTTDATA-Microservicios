package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collections;

import javax.validation.Validation;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.TacoOrder;
import tacos.User;
import tacos.api.dto.ReorderRequest;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.OrderMapper;
import tacos.data.OrderRepository;

//TC-24 - Confirmacion e identidad de recompra
public class Tc24ReorderValidationTest {
    @Test
    public void priceChangeDoesNotReserveBeforeConfirmation() {
        OrderRepository repo = mock(OrderRepository.class);
        OrderService orderService = mock(OrderService.class);
        TacoOrder original = order("old", "10.00", "alice");
        TacoOrder prepared = order(null, "12.00", "alice");
        ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
        when(template.findById(any(String.class), org.mockito.ArgumentMatchers.eq(tacos.ReorderOperation.class)))
            .thenReturn(Mono.empty());
        when(repo.findById("old")).thenReturn(Mono.just(original));
        when(orderService.prepareOrder(any(), any())).thenReturn(Mono.just(prepared));
        ReorderService service = new ReorderService(repo, orderService, mock(OrderMapper.class),
            template, Clock.systemUTC());

        StepVerifier.create(service.reorder("old", request(false), auth("alice")))
            .assertNext(response -> assertThat(response.isRequiresConfirmation()).isTrue())
            .verifyComplete();
        verify(orderService, never()).createPreparedOrder(any(), any());
    }

    @Test
    public void foreignOrderIsReportedAsNotFound() {
        OrderRepository repo = mock(OrderRepository.class);
        ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
        when(template.findById(any(String.class), org.mockito.ArgumentMatchers.eq(tacos.ReorderOperation.class)))
            .thenReturn(Mono.empty());
        when(repo.findById("old")).thenReturn(Mono.just(order("old", "10.00", "bob")));
        ReorderService service = new ReorderService(repo, mock(OrderService.class), mock(OrderMapper.class),
            template, Clock.systemUTC());
        StepVerifier.create(service.reorder("old", request(false), auth("alice")))
            .expectError(ResourceNotFoundException.class).verify();
    }

    @Test public void rejectsBlankReorderKey() { ReorderRequest r = request(false); r.setReorderKey(""); assertInvalid(r); }
    @Test public void rejectsBlankPaymentMethod() { ReorderRequest r = request(false); r.setPaymentMethodId(""); assertInvalid(r); }

    private void assertInvalid(ReorderRequest request) {
        assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(request)).isNotEmpty();
    }
    private ReorderRequest request(boolean confirm) {
        ReorderRequest request = new ReorderRequest(); request.setPaymentMethodId("pm1");
        request.setReorderKey("rk1"); request.setConfirmPriceChange(confirm); return request;
    }
    private TacoOrder order(String id, String total, String username) {
        TacoOrder order = new TacoOrder(); order.setId(id); order.setTotal(new BigDecimal(total));
        order.setCurrency("USD"); order.setItems(Collections.emptyList());
        order.setUser(new User(username, "hash", "User", "Street", "City", "ST", "000", "555", username + "@x.test"));
        return order;
    }
    private TestingAuthenticationToken auth(String name) {
        return new TestingAuthenticationToken(name, "pw", Collections.singletonList(
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
    }
}
//Fin TC-24
