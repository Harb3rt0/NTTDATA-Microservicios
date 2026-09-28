package tacos.payment;

import lombok.Getter;

//TC-12 - Resultado interno seguro producido por el gateway
@Getter
public class TokenizedPayment {
    private final String paymentToken;
    private final String brand;
    private final String last4;
    private final String expiration;

    public TokenizedPayment(String paymentToken, String brand, String last4, String expiration) {
        this.paymentToken = paymentToken;
        this.brand = brand;
        this.last4 = last4;
        this.expiration = expiration;
    }
}
//Fin TC-12
