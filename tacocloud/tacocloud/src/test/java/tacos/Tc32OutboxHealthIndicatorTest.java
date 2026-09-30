package tacos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;

//TC-32 - health degradado por outbox fallido
public class Tc32OutboxHealthIndicatorTest {
  @Test
  public void shouldReportDownWhenOutboxContainsFailedEvents() throws Exception {
    ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
    when(template.count(any(Query.class), eq(OutboxEvent.class)))
        .thenReturn(Mono.just(2L), Mono.just(1L), Mono.just(0L));
    org.springframework.boot.actuate.health.Health health =
        new OutboxHealthIndicator(template).health().toFuture().get();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("failed", 1L);
  }
}
//Fin TC-32
