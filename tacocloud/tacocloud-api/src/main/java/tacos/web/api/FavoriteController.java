package tacos.web.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.api.dto.FavoriteResponse;
import tacos.api.dto.PagedResponse;

//TC-21 - API de favoritos del usuario autenticado
@RestController
@Validated
@RequestMapping(path = "/api/users/me/favorites", produces = "application/json")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PutMapping("/{tacoId}")
    public Mono<FavoriteResponse> add(@PathVariable @NotBlank @Size(max = 64) String tacoId,
            Authentication authentication) {
        return favoriteService.add(tacoId, authentication);
    }

    @DeleteMapping("/{tacoId}")
    public Mono<ResponseEntity<Void>> remove(@PathVariable @NotBlank @Size(max = 64) String tacoId,
            Authentication authentication) {
        return favoriteService.remove(tacoId, authentication)
            .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping
    public Mono<PagedResponse<FavoriteResponse>> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        return favoriteService.list(page, size, authentication);
    }
}
//Fin TC-21
