package tacos.physics;

import java.util.List;

import lombok.Value;
import tacos.Ingredient;

//TC-18 - Contexto resuelto una vez por taco
@Value
public class TacoDesignContext {
    String name;
    List<Ingredient> ingredients;
}
//Fin TC-18
