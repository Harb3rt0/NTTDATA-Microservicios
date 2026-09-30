package tacos;

import java.time.Instant;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

//TC-33 - anuncio operativo persistente
@Data
@Document(collection = "ops_announcements")
public class OpsAnnouncement {
  @Id
  private String id;
  private String text;
  private AnnouncementSeverity severity;
  private Instant createdAt;
  @Indexed(expireAfterSeconds = 0)
  private Instant expiresAt;
  private String createdBy;
  private boolean active;
  @Indexed(unique = true, sparse = true)
  private Integer slot; //modificacion para TC-33
  @Version
  private Long version;
}
//Fin TC-33
