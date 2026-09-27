package tacos.api.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data 
@JsonIgnoreProperties(ignoreUnknown = true)
public class TacoCreateRequest {
    private String name;
    private List<String> ingredientIds = new ArrayList<>();
}
