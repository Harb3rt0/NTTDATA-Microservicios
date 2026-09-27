package tacos.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data 
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreateRequest {   //modificacion para TC-09
    @NotBlank(message = "Delivery name is required")
    @Size(max = 80, message = "Delivery name must not exceed 80 characters")
    private String deliveryName;

    @NotBlank(message = "Delivery street is required")
    @Size(max = 120, message = "Delivery street must not exceed 120 characters")
    private String deliveryStreet;

    @NotBlank(message = "Delivery city is required")
    @Size(max = 80, message = "Delivery city must not exceed 80 characters")
    private String deliveryCity;

    @NotBlank(message = "Delivery state is required")
    @Size(max = 50, message = "Delivery state must not exceed 50 characters")
    private String deliveryState;

    @NotBlank(message = "Delivery ZIP is required")
    @Size(min = 3, max = 12, message = "Delivery ZIP must contain between 3 and 12 characters")
    private String deliveryZip;

    @NotBlank(message = "Credit card number is required")
    @Pattern(regexp = "\\d{13,19}", message = "Credit card number must contain between 13 and 19 digits")
    private String ccNumber;

    @NotBlank(message = "Credit card expiration is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{2}$", message = "Credit card expiration must use MM/YY format")
    private String ccExpiration;

    @NotBlank(message = "Credit card CVV is required")
    @Pattern(regexp = "\\d{3,4}", message = "Credit card CVV must contain 3 or 4 digits")
    private String ccCVV;

    @Valid
    @NotEmpty(message = "At least one taco is required")
    @Size(max = 20, message = "An order cannot contain more than 20 tacos")
    private List<TacoCreateRequest> tacos = new ArrayList<>();
}
