package tacos.web.api;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.api.dto.ReorderRequest;
import tacos.api.dto.ReorderResponse;

//TC-24 - Endpoint de recompra con confirmacion de precio
@RestController
@Validated
@RequestMapping(path = "/api/orders", produces = "application/json")
public class ReorderController {
    private final ReorderService reorderService;

    public ReorderController(ReorderService reorderService) {
        this.reorderService = reorderService;
    }

    @PostMapping(path = "/{orderId}/reorder", consumes = "application/json")
    public Mono<ResponseEntity<ReorderResponse>> reorder(
            @PathVariable @NotBlank @Size(max = 64) String orderId,
            @Valid @RequestBody ReorderRequest request, Authentication authentication) {
        return reorderService.reorder(orderId, request, authentication).map(response ->
            ResponseEntity.status(response.getOrder() == null ? HttpStatus.OK : HttpStatus.CREATED)
                .body(response));
    }
}
//Fin TC-24
