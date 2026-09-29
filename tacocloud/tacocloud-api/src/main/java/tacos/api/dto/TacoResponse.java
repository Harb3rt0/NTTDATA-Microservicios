package tacos.api.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data 
public class TacoResponse {
    private String name;
    private List<IngredientResponse> ingredients = new ArrayList<>();
    private TacoClassificationResponse classification; //modificacion para TC-17
}
