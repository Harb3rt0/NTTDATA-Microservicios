package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

//TC-32 - metricas de negocio sin tags de alta cardinalidad
public class Tc32BusinessMetricsTest {
  @Test
  public void shouldCountAndTimeSuccessfulPlacement() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    BusinessMetrics metrics = new BusinessMetrics(registry);

    StepVerifier.create(metrics.timePlacement(Mono.just("created")))
        .expectNext("created").verifyComplete();

    assertThat(registry.counter("tacocloud.orders.created", "result", "success").count())
        .isEqualTo(1.0);
    assertThat(registry.timer("tacocloud.orders.placement", "result", "success").count())
        .isEqualTo(1L);
    assertThat(registry.getMeters()).allSatisfy(meter ->
        assertThat(meter.getId().getTags()).allSatisfy(tag ->
            assertThat(tag.getKey()).isNotIn("orderId", "userId", "eventId", "correlationId")));
  }

  @Test
  public void shouldExposeReactiveOutboxBacklogGauge() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    BusinessMetrics metrics = new BusinessMetrics(registry);
    metrics.updateOutboxBacklog(7);
    assertThat(registry.get("tacocloud.outbox.backlog").gauge().value()).isEqualTo(7.0);
  }
}
//Fin TC-32
