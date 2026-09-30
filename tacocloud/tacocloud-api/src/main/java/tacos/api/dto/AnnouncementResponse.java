package tacos.api.dto;

import java.time.Instant;

import lombok.Data;
import tacos.AnnouncementSeverity;

//TC-33 - respuesta publica sin identidad del autor
@Data
public class AnnouncementResponse {
  private String id;
  private String text;
  private AnnouncementSeverity severity;
  private Instant createdAt;
  private Instant expiresAt;
}
//Fin TC-33
