package tacos.api.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

//TC-26 - Linea operativa segura para cocina
@Data
public class KitchenOrderItemResponse {
  private String name;
  private int quantity;
  private List<String> ingredients = new ArrayList<>();
}
//Fin TC-26
