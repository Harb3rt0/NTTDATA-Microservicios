package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.AnnouncementSeverity;
import tacos.OpsAnnouncement;
import tacos.api.dto.AnnouncementRequest;
import tacos.api.error.BadRequestException;
import tacos.data.OpsAnnouncementRepository;

//TC-33 - persistencia, expiracion y validacion deterministas
public class Tc33OpsAnnouncementServiceTest {
  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  @Test
  public void shouldCreateSafeResponseWithoutAuthor() {
    OpsAnnouncementRepository repository = mock(OpsAnnouncementRepository.class);
    when(repository.deleteByExpiresAtLessThanEqual(NOW)).thenReturn(Mono.just(0L));
    when(repository.save(any(OpsAnnouncement.class))).thenAnswer(invocation -> {
      OpsAnnouncement value = invocation.getArgument(0);
      value.setId("A1");
      return Mono.just(value);
    });
    OpsAnnouncementService service = service(repository);

    StepVerifier.create(service.create(request("Maintenance", NOW.plusSeconds(3600)), "admin"))
        .assertNext(result -> {
          assertThat(result.getId()).isEqualTo("A1");
          assertThat(result.getText()).isEqualTo("Maintenance");
        }).verifyComplete();
    verify(repository).save(any(OpsAnnouncement.class));
  }

  @Test
  public void shouldRejectControlCharactersAndExpiredValuesBeforeSave() {
    OpsAnnouncementRepository repository = mock(OpsAnnouncementRepository.class);
    OpsAnnouncementService service = service(repository);
    StepVerifier.create(service.create(request("bad\rtext", NOW.plusSeconds(10)), "admin"))
        .expectError(BadRequestException.class).verify();
    StepVerifier.create(service.create(request("old", NOW), "admin"))
        .expectError(BadRequestException.class).verify();
  }

  private OpsAnnouncementService service(OpsAnnouncementRepository repository) {
    return new OpsAnnouncementService(repository, Clock.fixed(NOW, ZoneOffset.UTC), 2);
  }

  private AnnouncementRequest request(String text, Instant expiresAt) {
    AnnouncementRequest request = new AnnouncementRequest();
    request.setText(text);
    request.setSeverity(AnnouncementSeverity.INFO);
    request.setExpiresAt(expiresAt);
    return request;
  }
}
//Fin TC-33
