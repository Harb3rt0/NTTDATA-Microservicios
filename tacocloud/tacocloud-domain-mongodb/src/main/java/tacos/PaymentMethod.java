package tacos;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

//TC-12 - Metodo de pago tokenizado sin PAN ni CVV
@Document
@Getter
@Setter
@EqualsAndHashCode
public class PaymentMethod {

  @Id
  private String id;

  private final User user;
  @JsonIgnore
  @ToString.Exclude
  private final String paymentToken;
  private final String brand;
  private final String last4;
  private final String expiration;

  //modificacion para TC-14
  @PersistenceConstructor
  public PaymentMethod(User user, String paymentToken, String brand, String last4,
      String expiration) {
    this.user = user;
    this.paymentToken = paymentToken;
    this.brand = brand;
    this.last4 = last4;
    this.expiration = expiration;
  }

}
//Fin TC-12
