package tacos.web.api;

import java.time.Clock;
import java.time.Instant;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OpsAnnouncement;
import tacos.api.dto.AnnouncementRequest;
import tacos.api.dto.AnnouncementResponse;
import tacos.api.error.ConflictException;
import tacos.api.error.BadRequestException;
import tacos.api.error.ResourceNotFoundException;
import tacos.data.OpsAnnouncementRepository;

//TC-33 - reglas persistentes y acotadas de anuncios
@Service
public class OpsAnnouncementService {
  private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\t]]");
  private final OpsAnnouncementRepository repository;
  private final Clock clock;
  private final int maxActive;

  public OpsAnnouncementService(OpsAnnouncementRepository repository, Clock clock,
      @Value("${tacocloud.announcements.max-active:20}") int maxActive) {
    this.repository = repository;
    this.clock = clock;
    this.maxActive = maxActive;
  }

  public Flux<AnnouncementResponse> active() {
    return repository.findByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(clock.instant())
        .map(this::response);
  }

  public Mono<AnnouncementResponse> create(AnnouncementRequest request, String actor) {
    return Mono.defer(() -> {
      if (CONTROL.matcher(request.getText()).find()) {
        return Mono.error(new BadRequestException("INVALID_ANNOUNCEMENT_TEXT",
            "Announcement text contains control characters."));
      }
      if (!request.getExpiresAt().isAfter(clock.instant())) {
        return Mono.error(new BadRequestException("INVALID_ANNOUNCEMENT_EXPIRATION",
            "Announcement expiration must be in the future."));
      }
      return repository.deleteByExpiresAtLessThanEqual(clock.instant())
          .thenMany(Flux.range(0, maxActive)
              .concatMap(slot -> repository.save(entity(request, actor, slot))
                  .onErrorResume(DuplicateKeyException.class, error -> Mono.empty())))
          .next()
          .switchIfEmpty(Mono.error(new ConflictException("ANNOUNCEMENT_LIMIT_REACHED",
              "The active announcement limit was reached.")))
          .map(this::response); //modificacion para TC-33: slots unicos evitan exceder el limite concurrente
    });
  }

  public Mono<Void> delete(String id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
            "ANNOUNCEMENT_NOT_FOUND", "Announcement was not found.")))
        .flatMap(repository::delete);
  }

  private OpsAnnouncement entity(AnnouncementRequest request, String actor, int slot) {
    OpsAnnouncement value = new OpsAnnouncement();
    value.setText(request.getText().trim());
    value.setSeverity(request.getSeverity());
    value.setCreatedAt(clock.instant());
    value.setExpiresAt(request.getExpiresAt());
    value.setCreatedBy(actor);
    value.setActive(true);
    value.setSlot(slot);
    return value;
  }

  private AnnouncementResponse response(OpsAnnouncement value) {
    AnnouncementResponse response = new AnnouncementResponse();
    response.setId(value.getId());
    response.setText(value.getText());
    response.setSeverity(value.getSeverity());
    response.setCreatedAt(value.getCreatedAt());
    response.setExpiresAt(value.getExpiresAt());
    return response;
  }
}
//Fin TC-33
