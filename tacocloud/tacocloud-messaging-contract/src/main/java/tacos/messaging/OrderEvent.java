package tacos.messaging;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//TC-27 - Envolvente versionada del evento
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEvent implements Serializable {
  private static final long serialVersionUID = 1L;

  private String eventId;
  private OrderEventType type;
  private int version;
  private Date occurredAt;
  private String correlationId;
  private OrderEventPayload payload;

  public String getEventId() { return eventId; }
  public void setEventId(String eventId) { this.eventId = eventId; }
  public OrderEventType getType() { return type; }
  public void setType(OrderEventType type) { this.type = type; }
  public int getVersion() { return version; }
  public void setVersion(int version) { this.version = version; }
  public Date getOccurredAt() { return occurredAt; }
  public void setOccurredAt(Date occurredAt) { this.occurredAt = occurredAt; }
  public String getCorrelationId() { return correlationId; }
  public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
  public OrderEventPayload getPayload() { return payload; }
  public void setPayload(OrderEventPayload payload) { this.payload = payload; }
}
//Fin TC-27
