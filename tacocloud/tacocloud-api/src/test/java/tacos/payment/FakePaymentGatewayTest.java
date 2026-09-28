package tacos.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

//TC-12 - Pruebas del gateway fake con datos sinteticos
public class FakePaymentGatewayTest {

    @Test
    public void shouldCreateOpaqueTokenWithoutRetainingCardData() {
        String syntheticCardNumber = "9999999999999999";
        String syntheticCvv = "999";
        PaymentTokenizationCommand command = new PaymentTokenizationCommand(
            syntheticCardNumber, "12/39", syntheticCvv);

        StepVerifier.create(new FakePaymentGateway().tokenize(command))
            .assertNext(result -> {
                assertThat(result.getPaymentToken()).startsWith("labtok_");
                assertThat(result.getPaymentToken()).doesNotContain(syntheticCardNumber);
                assertThat(result.getPaymentToken()).doesNotContain(syntheticCvv);
                assertThat(result.getBrand()).isEqualTo("LAB_CARD");
                assertThat(result.getLast4()).isEqualTo("9999");
                assertThat(result.getExpiration()).isEqualTo("12/39");
            })
            .verifyComplete();
    }
}
//Fin TC-12
