package tacos.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.Getter;
import lombok.Setter;

//TC-12 - Solicitud sensible usada solo durante tokenizacion
@Getter
@Setter
public class PaymentMethodTokenizeRequest {
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{13,19}", message = "Card number must contain between 13 and 19 digits")
    private String cardNumber;

    @NotBlank(message = "Expiration is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{2}$", message = "Expiration must use MM/YY format")
    private String expiration;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "\\d{3,4}", message = "CVV must contain 3 or 4 digits")
    private String cvv;
}
//Fin TC-12
