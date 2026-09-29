package tacos.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

//TC-15 - Entrada sin efectos para cotizar una orden
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderQuoteRequest {
    @Valid
    @NotEmpty(message = "At least one item is required")
    @Size(max = 20, message = "An order cannot contain more than 20 items")
    private List<OrderLineCreateRequest> items = new ArrayList<>();

    @Size(max = 40, message = "Coupon code must not exceed 40 characters")
    private String couponCode;
}
//Fin TC-15
