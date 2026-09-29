package tacos.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data 
@JsonIgnoreProperties(ignoreUnknown = true)
public class TacoCreateRequest {    //modificacion para TC-09
    @NotBlank(message = "Taco name is required")
    @Size(max = 50, message = "Taco name must not exceed 50 characters")
    private String name;

    @NotNull(message = "Ingredient ids are required")
    @Size(max = 12, message = "A taco cannot contain more than 12 ingredients") //modificacion para TC-18
    private List<
        @NotBlank(message = "Ingredient id cannot be blank")
        @Size(max = 20, message = "Ingredient id must not exceed 20 characters")
        String
    > ingredientIds =
        new ArrayList<>();
}
