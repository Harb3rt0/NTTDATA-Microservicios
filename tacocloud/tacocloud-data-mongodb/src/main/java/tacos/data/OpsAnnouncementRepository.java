package tacos.data;

import java.time.Instant;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OpsAnnouncement;

//TC-33 - persistencia reactiva de anuncios
public interface OpsAnnouncementRepository extends ReactiveCrudRepository<OpsAnnouncement, String> {
  Flux<OpsAnnouncement> findByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(Instant now);
  Mono<Long> countByActiveTrueAndExpiresAtAfter(Instant now);
  Mono<Long> deleteByExpiresAtLessThanEqual(Instant now); //modificacion para TC-33
}
//Fin TC-33
