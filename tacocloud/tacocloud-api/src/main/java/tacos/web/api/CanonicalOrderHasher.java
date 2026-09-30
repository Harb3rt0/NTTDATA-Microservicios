package tacos.web.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tacos.api.dto.OrderCreateRequest;

//TC-34 - hash SHA-256 de una representacion logica canonica
@Component
public class CanonicalOrderHasher {
  public String hash(OrderCreateRequest request) {
    String items = request.getItems().stream().map(item -> {
      String ingredients = item.getTaco().getIngredientIds().stream().sorted()
          .collect(Collectors.joining(","));
      return normalize(item.getTaco().getName()) + "|" + ingredients + "|" + item.getQuantity();
    }).sorted(Comparator.naturalOrder()).collect(Collectors.joining(";"));
    String canonical = String.join("\n", normalize(request.getDeliveryName()),
        normalize(request.getDeliveryStreet()), normalize(request.getDeliveryCity()),
        normalize(request.getDeliveryState()), normalize(request.getDeliveryZip()),
        normalize(request.getPaymentMethodId()), normalize(request.getCouponCode()), items);
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(canonical.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte value : digest) {
        result.append(String.format("%02x", value));
      }
      return result.toString();
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 is unavailable.", error);
    }
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
//Fin TC-34
