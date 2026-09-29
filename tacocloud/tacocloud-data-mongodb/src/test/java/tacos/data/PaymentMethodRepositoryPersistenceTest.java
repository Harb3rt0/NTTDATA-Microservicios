package tacos.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import reactor.test.StepVerifier;
import tacos.PaymentMethod;
import tacos.User;

//TC-14 - Verifica que el metodo tokenizado pueda recuperarse antes de crear la orden
@DataMongoTest
public class PaymentMethodRepositoryPersistenceTest {

  @Autowired
  private PaymentMethodRepository paymentMethodRepo;

  @Test
  public void shouldSaveAndReadPaymentMethodWithItsOwner() {
    User user = new User("user-a", "{bcrypt}hash", "Usuario A", "Calle Pruebas 100",
        "Guadalajara", "Jalisco", "44100", "5550101000", "user-a@example.test");
    PaymentMethod paymentMethod = new PaymentMethod(user, "labtok_test", "LAB_CARD",
        "9999", "12/39");

    StepVerifier.create(paymentMethodRepo.save(paymentMethod)
            .flatMap(saved -> paymentMethodRepo.findById(saved.getId())))
        .assertNext(saved -> {
          assertNotNull(saved.getId());
          assertEquals("user-a", saved.getUser().getUsername());
          assertEquals("labtok_test", saved.getPaymentToken());
          assertEquals("LAB_CARD", saved.getBrand());
          assertEquals("9999", saved.getLast4());
        })
        .verifyComplete();
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class TestApplication {
  }
}
//Fin TC-14
