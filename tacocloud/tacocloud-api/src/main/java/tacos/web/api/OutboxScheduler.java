package tacos.web.api;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//TC-29 - Borde programado que inicia el publisher reactivo
@Component
public class OutboxScheduler {
  private final OutboxPublisher publisher;

  public OutboxScheduler(OutboxPublisher publisher) {
    this.publisher = publisher;
  }

  @Scheduled(fixedDelayString = "${tacocloud.outbox.poll-delay-ms:1000}")
  public void publish() {
    publisher.publishBatch().subscribe();
  }
}
//Fin TC-29
