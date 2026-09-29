package tacos.web.api;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.api.dto.TacoCreateRequest;
import tacos.api.dto.TacoValidationResponse;

//TC-18 - Endpoint de validacion sin persistencia
@RestController
@RequestMapping(path = "/api/tacos", produces = "application/json")
public class TacoValidationController {
    private final TacoDesignService tacoDesignService;

    public TacoValidationController(TacoDesignService tacoDesignService) {
        this.tacoDesignService = tacoDesignService;
    }

    @PostMapping(path = "/validate", consumes = "application/json")
    public Mono<TacoValidationResponse> validate(@Valid @RequestBody TacoCreateRequest request) {
        return tacoDesignService.validate(request);
    }
}
//Fin TC-18
