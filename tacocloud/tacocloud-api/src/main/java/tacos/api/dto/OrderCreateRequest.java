package tacos.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
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

    @NotBlank(message = "Payment method is required")
    @Size(max = 64, message = "Payment method identifier must not exceed 64 characters")
    private String paymentMethodId; //modificacion para TC-12

    @Valid
    //modificacion para TC-14
    @NotEmpty(message = "At least one item is required")
    @Size(max = 20, message = "An order cannot contain more than 20 items")
    private List<OrderLineCreateRequest> items = new ArrayList<>();
}
