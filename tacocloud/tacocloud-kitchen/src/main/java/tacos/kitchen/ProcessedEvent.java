package tacos.kitchen;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

//TC-30 - Marcador durable de idempotencia
@Data
@Document(collection = "processed_events")
public class ProcessedEvent {
  @Id
  private String id;
  @Indexed(unique = true)
  private String eventId;
  private String eventType;
  private Date processedAt;
}
//Fin TC-30
