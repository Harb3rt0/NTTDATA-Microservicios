package tacos.web.api;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.api.dto.AnnouncementRequest;
import tacos.api.dto.AnnouncementResponse;

//TC-33 - CRUD REST administrativo de anuncios
@RestController
@RequestMapping(path = "/api/admin/announcements", produces = "application/json")
public class OpsAnnouncementController {
  private final OpsAnnouncementService service;

  public OpsAnnouncementController(OpsAnnouncementService service) {
    this.service = service;
  }

  @GetMapping
  public Flux<AnnouncementResponse> active() {
    return service.active();
  }

  @PostMapping(consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<AnnouncementResponse> create(@Valid @RequestBody AnnouncementRequest request,
      Authentication authentication) {
    return service.create(request, authentication.getName());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(@PathVariable String id) {
    return service.delete(id);
  }
}
//Fin TC-33
