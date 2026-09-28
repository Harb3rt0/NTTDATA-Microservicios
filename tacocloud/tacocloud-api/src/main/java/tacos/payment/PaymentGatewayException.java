package tacos.payment;

//TC-12 - Error seguro del gateway sin datos del pago
public class PaymentGatewayException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PaymentGatewayException(String message) {
        super(message);
    }
}
//Fin TC-12
