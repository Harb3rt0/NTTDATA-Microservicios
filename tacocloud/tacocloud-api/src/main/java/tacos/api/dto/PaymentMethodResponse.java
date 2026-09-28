package tacos.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//TC-12 - Respuesta publica de metodo de pago sin token ni datos de tarjeta
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {
    private String id;
    private String brand;
    private String last4;
}
//Fin TC-12
