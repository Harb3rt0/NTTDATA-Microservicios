package tacos.api.mapper;

import org.springframework.stereotype.Component;

import tacos.Ingredient;
import tacos.api.dto.IngredientAdminResponse;
import tacos.api.dto.IngredientCatalogUpdateRequest;
import tacos.api.dto.IngredientRequest;
import tacos.api.dto.IngredientResponse;

@Component 
public class IngredientMapper {
    public Ingredient toEntity(IngredientRequest request){
        return new Ingredient(request.getId(), request.getName(), request.getType(),
            request.getUnitPrice(), request.getAvailable(), request.getStockOnHand(),
            request.getReorderLevel()); //modificacion para TC-13
    }

    public void updateEntity(IngredientRequest request, Ingredient ingredient){
        ingredient.setName(request.getName());
        ingredient.setType(request.getType());
        ingredient.setUnitPrice(request.getUnitPrice()); //modificacion para TC-13
        ingredient.setAvailable(request.getAvailable());
        ingredient.setStockOnHand(request.getStockOnHand());
        ingredient.setReorderLevel(request.getReorderLevel());
    }

    public IngredientResponse toResponse(Ingredient ingredient){
        IngredientResponse response = new IngredientResponse();
        response.setId(ingredient.getId());
        response.setName(ingredient.getName());
        response.setType(ingredient.getType());
        response.setUnitPrice(ingredient.getUnitPrice()); //modificacion para TC-13
        response.setAvailable(ingredient.isAvailable());
        response.setDietaryTags(ingredient.getDietaryTags()); //modificacion para TC-17
        response.setAllergens(ingredient.getAllergens());
        response.setSpiceLevel(ingredient.getSpiceLevel());
        return response;
    }

    //TC-13 - Aplica solo los campos permitidos del catalogo
    public void updateCatalog(IngredientCatalogUpdateRequest request, Ingredient ingredient) {
        if (request.getUnitPrice() != null) {
            ingredient.setUnitPrice(request.getUnitPrice());
        }
        if (request.getAvailable() != null) {
            ingredient.setAvailable(request.getAvailable());
        }
        if (request.getReorderLevel() != null) {
            ingredient.setReorderLevel(request.getReorderLevel());
        }
    }
    //Fin TC-13

    //TC-13 - Mapea la vista administrativa del ingrediente
    public IngredientAdminResponse toAdminResponse(Ingredient ingredient) {
        IngredientAdminResponse response = new IngredientAdminResponse();
        response.setId(ingredient.getId());
        response.setName(ingredient.getName());
        response.setType(ingredient.getType());
        response.setUnitPrice(ingredient.getUnitPrice());
        response.setAvailable(ingredient.isAvailable());
        response.setStockOnHand(ingredient.getStockOnHand());
        response.setReorderLevel(ingredient.getReorderLevel());
        response.setVersion(ingredient.getVersion());
        return response;
    }
    //Fin TC-13
}
