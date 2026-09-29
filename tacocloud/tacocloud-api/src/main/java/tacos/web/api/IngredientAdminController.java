package tacos.web.api;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.api.dto.IngredientAdminResponse;
import tacos.api.dto.IngredientCatalogUpdateRequest;
import tacos.api.dto.StockAdjustmentRequest;
import tacos.api.mapper.IngredientMapper;

//TC-13 - API administrativa de catalogo e inventario
@RestController
@Validated
@RequestMapping(path = "/api/admin/ingredients", produces = "application/json")
public class IngredientAdminController {
    private final IngredientCatalogService ingredientCatalogService;
    private final IngredientMapper ingredientMapper;

    public IngredientAdminController(IngredientCatalogService ingredientCatalogService,
            IngredientMapper ingredientMapper) {
        this.ingredientCatalogService = ingredientCatalogService;
        this.ingredientMapper = ingredientMapper;
    }

    @PatchMapping(path = "/{id}/catalog", consumes = "application/json")
    public Mono<IngredientAdminResponse> updateCatalog(
            @PathVariable @NotBlank @Size(max = 20) String id,
            @Valid @RequestBody IngredientCatalogUpdateRequest request) {
        return ingredientCatalogService.updateCatalog(id, request)
            .map(ingredientMapper::toAdminResponse);
    }

    @PostMapping(path = "/{id}/stock-adjustments", consumes = "application/json")
    public Mono<IngredientAdminResponse> adjustStock(
            @PathVariable @NotBlank @Size(max = 20) String id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ingredientCatalogService.adjustStock(id, request)
            .map(ingredientMapper::toAdminResponse);
    }
}
//Fin TC-13
