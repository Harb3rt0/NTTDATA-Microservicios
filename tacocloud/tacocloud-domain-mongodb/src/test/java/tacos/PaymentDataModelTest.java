package tacos;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

//TC-12 - Modelo persistente sin PAN ni CVV
public class PaymentDataModelTest {

    @Test
    public void shouldNotDeclareSensitiveCardFieldsInOrderOrPaymentMethod() {
        Set<String> orderFields = fieldNames(TacoOrder.class);
        Set<String> paymentFields = fieldNames(PaymentMethod.class);

        assertThat(orderFields).doesNotContain("ccNumber", "ccCVV", "ccExpiration",
            "cardNumber", "cvv", "pan");
        assertThat(paymentFields).doesNotContain("ccNumber", "ccCVV", "ccExpiration",
            "cardNumber", "cvv", "pan");
        assertThat(paymentFields).contains("paymentToken", "brand", "last4", "expiration");
    }

    @Test
    public void shouldHideInternalTokenFromAccidentalJsonSerialization() throws Exception {
        User user = new User("alice", "{bcrypt}hash", "Test User", "Street", "City",
            "TX", "78701", "555-0100", "alice@example.com");
        PaymentMethod paymentMethod = new PaymentMethod(user, "labtok_internal_secret",
            "LAB_CARD", "9999", "12/39");

        String json = new ObjectMapper().writeValueAsString(paymentMethod);

        assertThat(json).doesNotContain("labtok_internal_secret", "paymentToken", "password");
    }

    private Set<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).map(Field::getName)
            .collect(Collectors.toSet());
    }
}
//Fin TC-12
