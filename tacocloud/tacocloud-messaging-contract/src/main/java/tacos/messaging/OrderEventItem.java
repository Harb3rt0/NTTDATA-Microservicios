package tacos.messaging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//TC-27 - Linea segura del snapshot de cocina
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEventItem implements Serializable {
  private static final long serialVersionUID = 1L;

  private String name;
  private int quantity;
  private List<String> ingredients = new ArrayList<>();

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
  public List<String> getIngredients() { return ingredients; }
  public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
}
//Fin TC-27
