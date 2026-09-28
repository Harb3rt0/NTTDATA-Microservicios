package tacos;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

//TC-12 - Metodo de pago tokenizado sin PAN ni CVV
@Document
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor(force=true, access=AccessLevel.PRIVATE)
@RequiredArgsConstructor
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

}
//Fin TC-12
