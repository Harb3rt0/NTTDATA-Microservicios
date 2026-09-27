package tacos.web.api;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class EmailOrder {
  //modificado para TC-09
  @NotBlank(message = "Email is required")
  @Email(message = "Email has an invalid format")
  private String email;

  @Valid
  @NotEmpty(message = "At least one taco is required")
  @Size(max = 20)
  private List<EmailTaco> tacos;

  @Data
  public static class EmailTaco {

    @NotBlank(message = "Taco name is required")
    @Size(max = 50)
    private String name;

    @NotEmpty(message = "At least one ingredient is required")
    @Size(max = 10)
    private List<
      @NotBlank(message = "Ingredient id cannot be blank")
      @Size(max = 20, message = "Ingredient id must not exceed 20 characters")
      String
    > ingredients;
  }
  
}
