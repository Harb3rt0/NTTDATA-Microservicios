package tacos.api.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data 
public class TacoResponse {
    //TC-19 - Identidad y fecha necesarias para catalogo estable
    private String id;
    private Date createdAt;
    //Fin TC-19
    private String name;
    private List<IngredientResponse> ingredients = new ArrayList<>();
    private TacoClassificationResponse classification; //modificacion para TC-17
}
