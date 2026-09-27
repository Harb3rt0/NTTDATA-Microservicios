package tacos.api.dto;

import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;

import lombok.Data;

@Data
public class OrderPatchRequest {        //modificacion para TC-09
    @Size(min = 1, max = 80, message = "Delivery name must contain between 1 and 80 characters")
    @Pattern(regexp = ".*\\S.*", message = "Delivery name cannot be blank")
    private String deliveryName;

    @Size(min = 1, max = 120, message = "Delivery street must contain between 1 and 120 characters")
    @Pattern(regexp = ".*\\S.*", message = "Delivery street cannot be blank")
    private String deliveryStreet;

    @Size(min = 1, max = 80, message = "Delivery city must contain between 1 and 80 characters")
    @Pattern(regexp = ".*\\S.*", message = "Delivery city cannot be blank")
    private String deliveryCity;

    @Size(min = 1, max = 50, message = "Delivery state must contain between 1 and 50 characters")
    @Pattern(regexp = ".*\\S.*", message = "Delivery state cannot be blank")
    private String deliveryState;

    @Size(min = 3, max = 12, message = "Delivery ZIP must contain between 3 and 12 characters")
    @Pattern(regexp = ".*\\S.*", message = "Delivery ZIP cannot be blank")
    private String deliveryZip;
}
