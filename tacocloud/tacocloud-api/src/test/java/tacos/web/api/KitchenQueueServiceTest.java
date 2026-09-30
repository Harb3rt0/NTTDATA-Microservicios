package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.OrderStatus;
import tacos.TacoOrder;

//TC-26 - Claim atomico unico y cola vacia definida
public class KitchenQueueServiceTest {
  @Test
  public void shouldClaimOnlyOneOrderWithAtomicFindAndModify() {
    ReactiveMongoTemplate template = Mockito.mock(ReactiveMongoTemplate.class);
    TacoOrder claimed = new TacoOrder();
    claimed.setId("O1");
    claimed.setStatus(OrderStatus.ACCEPTED);
    when(template.findAndModify(any(Query.class), any(Update.class),
        any(FindAndModifyOptions.class), eq(TacoOrder.class))).thenReturn(Mono.just(claimed));
    KitchenQueueService service = new KitchenQueueService(template, 50, 5, 4, 2, 1);

    StepVerifier.create(service.claim(auth())).assertNext(response -> {
      assertThat(response.getOrderId()).isEqualTo("O1");
      assertThat(response.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }).verifyComplete();

    verify(template, times(1)).findAndModify(any(Query.class), any(Update.class),
        any(FindAndModifyOptions.class), eq(TacoOrder.class));
  }

  @Test
  public void shouldReturnEmptyWhenNothingCanBeClaimed() {
    ReactiveMongoTemplate template = Mockito.mock(ReactiveMongoTemplate.class);
    when(template.findAndModify(any(Query.class), any(Update.class),
        any(FindAndModifyOptions.class), eq(TacoOrder.class))).thenReturn(Mono.empty());
    KitchenQueueService service = new KitchenQueueService(template, 50, 5, 4, 2, 1);

    StepVerifier.create(service.claim(auth())).verifyComplete();
  }

  private UsernamePasswordAuthenticationToken auth() {
    return new UsernamePasswordAuthenticationToken("cook", "n/a",
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_KITCHEN")));
  }
}
//Fin TC-26
