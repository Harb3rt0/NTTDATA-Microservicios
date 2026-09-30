package tacos.kitchen;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

//TC-26 - Pantalla minima que opera sobre la API de cola
@Controller
public class KitchenOperationsController {
  private final String apiBase;

  public KitchenOperationsController(
      @Value("${tacocloud.api-base-url:http://localhost:8080}") String apiBase) {
    this.apiBase = apiBase;
  }

  @GetMapping("/kitchen")
  public String kitchen(Model model) {
    model.addAttribute("apiBase", apiBase);
    return "kitchen";
  }
}
//Fin TC-26
