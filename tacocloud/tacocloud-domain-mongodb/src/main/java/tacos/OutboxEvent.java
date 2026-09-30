package tacos;

import java.io.Serializable;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

//TC-29 - Registro durable para publicacion al menos una vez
@Data
@Document(collection = "outbox_events")
public class OutboxEvent implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  private String id;

  @Indexed(unique = true)
  private String eventId;
  private String eventType;
  private int eventVersion;
  private String aggregateId;
  private String correlationId;
  private String payload;
  private OutboxStatus status;
  private int attempts;
  private Date createdAt;
  private Date updatedAt;
  private Date nextAttemptAt;
  private Date publishedAt;
  private String lastError;
}
//Fin TC-29
