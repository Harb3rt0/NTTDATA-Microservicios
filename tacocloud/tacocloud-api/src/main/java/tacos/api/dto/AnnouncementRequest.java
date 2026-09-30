package tacos.api.dto;

import java.time.Instant;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import tacos.AnnouncementSeverity;

//TC-33 - entrada validada para anuncios operativos
@Data
public class AnnouncementRequest {
  @NotBlank
  @Size(max = 500)
  private String text;
  @NotNull
  private AnnouncementSeverity severity;
  @NotNull
  @Future
  private Instant expiresAt;
}
//Fin TC-33
