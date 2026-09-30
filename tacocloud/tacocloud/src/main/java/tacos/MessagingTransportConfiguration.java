package tacos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

//TC-28 - Valida un solo transporte y bloquea noop en produccion
@Configuration
public class MessagingTransportConfiguration {
  private static final Set<String> ALLOWED = new HashSet<>(
      Arrays.asList("noop", "jms", "rabbit", "kafka"));

  @Bean
  public MessagingTransportValidator messagingTransportValidator(
      @Value("${tacocloud.messaging.transport:noop}") String transport,
      Environment environment) {
    if (!ALLOWED.contains(transport)) {
      throw new IllegalStateException("Unsupported tacocloud.messaging.transport: " + transport);
    }
    boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    if (production && "noop".equals(transport)) {
      throw new IllegalStateException("The noop messaging transport is not allowed in prod.");
    }
    return new MessagingTransportValidator(transport);
  }

  public static final class MessagingTransportValidator {
    private final String transport;

    MessagingTransportValidator(String transport) {
      this.transport = transport;
    }

    public String getTransport() {
      return transport;
    }
  }
}
//Fin TC-28
