package tacos.web.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.api.dto.KitchenOrderResponse;

//TC-26 - API operacional de cocina
@RestController
@RequestMapping(path = "/api/kitchen", produces = "application/json")
public class KitchenQueueController {
  private final KitchenQueueService queueService;

  public KitchenQueueController(KitchenQueueService queueService) {
    this.queueService = queueService;
  }

  @GetMapping("/queue")
  public Flux<KitchenOrderResponse> queue(Authentication authentication) {
    return queueService.queue(authentication);
  }

  @PostMapping("/orders/claim")
  public Mono<ResponseEntity<KitchenOrderResponse>> claim(Authentication authentication) {
    return queueService.claim(authentication).map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.noContent().build());
  }
}
//Fin TC-26
