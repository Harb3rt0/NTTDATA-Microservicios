package tacos;

import java.time.Instant;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

//TC-34 - registro durable de idempotencia por usuario y key
@Data
@Document(collection = "idempotency_records")
@CompoundIndex(name = "idempotency_user_key_unique", def = "{'userId':1,'key':1}", unique = true)
public class IdempotencyRecord {
  @Id
  private String id;
  private String userId;
  private String key;
  private String requestHash;
  private String orderId;
  private IdempotencyStatus status;
  private Instant createdAt;
  private Instant updatedAt;
  @Indexed(expireAfterSeconds = 0)
  private Instant expiresAt;
}
//Fin TC-34
