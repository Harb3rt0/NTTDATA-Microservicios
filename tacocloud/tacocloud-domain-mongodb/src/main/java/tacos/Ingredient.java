package tacos;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(access=AccessLevel.PRIVATE, force=true)
@Document
public class Ingredient {

  @Id
  @NotBlank(message = "Ingredient id is required")
  @Size(max = 20, message = "Ingredient id must not exceed 20 characters")
  private String id;

  @NotBlank(message = "Ingredient name is required")
  @Size(max = 50, message = "Ingredient name must not exceed 50 characters")
  private String name;

  @NotNull(message = "Ingredient type is required")
  private Type type;

  public enum Type {
    WRAP, PROTEIN, VEGGIES, CHEESE, SAUCE
  }

}
