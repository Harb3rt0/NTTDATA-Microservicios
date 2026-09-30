package tacos.api;

import java.util.UUID;
import java.util.regex.Pattern;

//TC-31 - valida o genera identificadores de correlacion seguros
public final class CorrelationIds {
  public static final String HEADER = "X-Correlation-Id";
  public static final String ATTRIBUTE = "tacos.api.CorrelationIds";
  private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

  private CorrelationIds() {
  }

  public static String resolve(String candidate) {
    return candidate != null && VALID.matcher(candidate).matches()
        ? candidate : UUID.randomUUID().toString();
  }
}
//Fin TC-31
