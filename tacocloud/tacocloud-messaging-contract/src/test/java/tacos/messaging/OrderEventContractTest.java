package tacos.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

//TC-27 - Compatibilidad JSON v1 y ausencia de datos sensibles
public class OrderEventContractTest {
  @Test
  public void shouldReadV1WithUnknownFieldsAndKeepSafePayload() throws Exception {
    String json = "{\"eventId\":\"E1\",\"type\":\"CREATED\",\"version\":1,"
        + "\"unknown\":true,\"payload\":{\"orderId\":\"O1\",\"status\":\"CREATED\","
        + "\"anotherUnknown\":\"ok\",\"items\":[]}}";
    ObjectMapper mapper = new ObjectMapper();

    OrderEvent event = mapper.readValue(json, OrderEvent.class);
    String serialized = mapper.writeValueAsString(event);

    assertEquals("O1", event.getPayload().getOrderId());
    assertFalse(serialized.toLowerCase().matches(".*(password|payment|cvv|card).*"));
  }
}
//Fin TC-27
