package tacos.web.api;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.api.dto.RatingRequest;
import tacos.api.dto.RatingSummaryResponse;

//TC-22 - API de voto y ranking publico
@RestController
@Validated
@RequestMapping(path = "/api/tacos", produces = "application/json")
public class TacoRatingController {
    private final TacoRatingService ratingService;

    public TacoRatingController(TacoRatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PutMapping(path = "/{tacoId}/rating", consumes = "application/json")
    public Mono<RatingSummaryResponse> rate(@PathVariable @NotBlank @Size(max = 64) String tacoId,
            @Valid @RequestBody RatingRequest request, Authentication authentication) {
        return ratingService.rate(tacoId, request.getScore(), authentication);
    }

    @GetMapping("/top")
    public Flux<RatingSummaryResponse> top(@RequestParam(defaultValue = "10") int limit) {
        return ratingService.top(limit);
    }
}
//Fin TC-22
