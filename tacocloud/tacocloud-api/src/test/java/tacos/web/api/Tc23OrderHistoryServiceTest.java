package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.TacoOrder;
import tacos.api.error.BadRequestException;
import tacos.api.mapper.OrderMapper;
import tacos.data.OrderRepository;

//TC-23 - Historial propio paginado
public class Tc23OrderHistoryServiceTest {
    @Test
    public void queriesOnlyAuthenticatedOwner() {
        OrderRepository repo = mock(OrderRepository.class);
        when(repo.findByUserUsernameOrderByPlacedAtDescIdAsc(any(), any()))
            .thenReturn(Flux.just(order("o1")));
        when(repo.countByUserUsername("alice")).thenReturn(Mono.just(1L));
        OrderHistoryService service = new OrderHistoryService(repo, mock(OrderMapper.class));

        StepVerifier.create(service.mine(0, 10, auth("alice")))
            .assertNext(page -> assertThat(page.getTotalElements()).isEqualTo(1)).verifyComplete();
        verify(repo).findByUserUsernameOrderByPlacedAtDescIdAsc(org.mockito.ArgumentMatchers.eq("alice"), any());
    }

    @Test
    public void outOfRangePageIsEmpty() {
        OrderRepository repo = mock(OrderRepository.class);
        when(repo.findByUserUsernameOrderByPlacedAtDescIdAsc(any(), any())).thenReturn(Flux.empty());
        when(repo.countByUserUsername("alice")).thenReturn(Mono.just(1L));
        StepVerifier.create(new OrderHistoryService(repo, mock(OrderMapper.class)).mine(5, 10, auth("alice")))
            .assertNext(page -> assertThat(page.getItems()).isEmpty()).verifyComplete();
    }

    @Test public void rejectsNegativePage() { assertInvalid(-1, 10); }
    @Test public void rejectsLargeSize() { assertInvalid(0, 51); }

    private void assertInvalid(int page, int size) {
        OrderHistoryService service = new OrderHistoryService(mock(OrderRepository.class),
            mock(OrderMapper.class));
        assertThatThrownBy(() -> service.mine(page, size, auth("alice")))
            .isInstanceOf(BadRequestException.class);
    }
    private TestingAuthenticationToken auth(String name) {
        return new TestingAuthenticationToken(name, "pw", Collections.singletonList(
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
    }
    private TacoOrder order(String id) {
        TacoOrder order = new TacoOrder(); order.setId(id); order.setItems(Collections.emptyList());
        order.setTotal(new BigDecimal("10.00")); order.setCurrency("USD"); return order;
    }
}
//Fin TC-23
