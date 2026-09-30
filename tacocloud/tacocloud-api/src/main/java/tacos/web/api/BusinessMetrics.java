package tacos.web.api;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

//TC-32 - metricas de negocio con tags de baja cardinalidad
@Component
public class BusinessMetrics {
  private final MeterRegistry registry;
  private final AtomicLong outboxBacklog = new AtomicLong();

  public BusinessMetrics(MeterRegistry registry) {
    this.registry = registry;
    Gauge.builder("tacocloud.outbox.backlog", outboxBacklog, AtomicLong::get)
        .description("Pending outbox events").register(registry);
  }

  public <T> Mono<T> timePlacement(Mono<T> operation) {
    return Mono.defer(() -> {
      Timer.Sample sample = Timer.start(registry);
      return operation.doOnSuccess(value -> {
        sample.stop(timer("success"));
        counter("tacocloud.orders.created", "result", "success").increment();
      }).doOnError(error -> {
        sample.stop(timer("failure"));
        counter("tacocloud.orders.failed", "result", "failure").increment();
      });
    });
  }

  public void orderCancelled() {
    counter("tacocloud.orders.cancelled", "result", "success").increment();
  }

  public void couponApplied() {
    counter("tacocloud.coupons.applied", "result", "success").increment();
  }

  public void stockRejected() {
    counter("tacocloud.inventory.rejected", "result", "rejected").increment();
  }

  public void updateOutboxBacklog(long value) {
    outboxBacklog.set(value);
  }

  private Counter counter(String name, String tag, String value) {
    return registry.counter(name, tag, value);
  }

  private Timer timer(String result) {
    return Timer.builder("tacocloud.orders.placement")
        .description("Order placement latency").tag("result", result).register(registry);
  }
}
//Fin TC-32
