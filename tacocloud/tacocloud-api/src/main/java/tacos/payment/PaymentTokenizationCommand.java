package tacos.payment;

import lombok.Getter;

//TC-12 - Datos efimeros entregados al gateway de pago
@Getter
public class PaymentTokenizationCommand {
    private final String cardNumber;
    private final String expiration;
    private final String cvv;

    public PaymentTokenizationCommand(String cardNumber, String expiration, String cvv) {
        this.cardNumber = cardNumber;
        this.expiration = expiration;
        this.cvv = cvv;
    }
}
//Fin TC-12
