package tacos.api.dto;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

//TC-14 - Entrada segura para una linea con cantidad
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderLineCreateRequest {
    @Valid
    @NotNull(message = "Taco is required")
    private TacoCreateRequest taco;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
//Fin TC-14
