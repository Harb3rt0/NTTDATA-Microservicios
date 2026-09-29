package tacos;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import tacos.Ingredient.Type;
import tacos.data.IngredientRepository;
import tacos.data.TacoRepository;
import tacos.data.UserRepository;

@Profile("!prod")
@Configuration
public class DevelopmentConfig {

  @Bean
  public CommandLineRunner dataLoader(IngredientRepository repo,
        UserRepository userRepo, PasswordEncoder encoder, TacoRepository tacoRepo) { //modificacion para TC-12
    
    return new CommandLineRunner() {
      @Override
      public void run(String... args) throws Exception {
        //modificacion para TC-17
        Ingredient flourTortilla = saveAnIngredient("FLTO", "Flour Tortilla", Type.WRAP, "1.25",
            EnumSet.of(DietaryTag.VEGAN, DietaryTag.VEGETARIAN),
            EnumSet.of(Allergen.GLUTEN), SpiceLevel.NONE);
        Ingredient cornTortilla = saveAnIngredient("COTO", "Corn Tortilla", Type.WRAP, "1.10",
            EnumSet.allOf(DietaryTag.class), EnumSet.noneOf(Allergen.class), SpiceLevel.NONE);
        Ingredient groundBeef = saveAnIngredient("GRBF", "Ground Beef", Type.PROTEIN, "2.35",
            EnumSet.of(DietaryTag.GLUTEN_FREE), EnumSet.noneOf(Allergen.class), SpiceLevel.NONE);
        Ingredient carnitas = saveAnIngredient("CARN", "Carnitas", Type.PROTEIN, "2.50",
            EnumSet.of(DietaryTag.GLUTEN_FREE), EnumSet.noneOf(Allergen.class), SpiceLevel.NONE);
        Ingredient tomatoes = saveAnIngredient("TMTO", "Diced Tomatoes", Type.VEGGIES, "0.75",
            EnumSet.allOf(DietaryTag.class), EnumSet.noneOf(Allergen.class), SpiceLevel.NONE);
        Ingredient lettuce = saveAnIngredient("LETC", "Lettuce", Type.VEGGIES, "0.65",
            EnumSet.allOf(DietaryTag.class), EnumSet.noneOf(Allergen.class), SpiceLevel.NONE);
        Ingredient cheddar = saveAnIngredient("CHED", "Cheddar", Type.CHEESE, "1.20",
            EnumSet.of(DietaryTag.VEGETARIAN, DietaryTag.GLUTEN_FREE),
            EnumSet.of(Allergen.DAIRY), SpiceLevel.NONE);
        Ingredient jack = saveAnIngredient("JACK", "Monterrey Jack", Type.CHEESE, "1.30",
            EnumSet.of(DietaryTag.VEGETARIAN, DietaryTag.GLUTEN_FREE),
            EnumSet.of(Allergen.DAIRY), SpiceLevel.NONE);
        Ingredient salsa = saveAnIngredient("SLSA", "Salsa", Type.SAUCE, "0.80",
            EnumSet.allOf(DietaryTag.class), EnumSet.noneOf(Allergen.class), SpiceLevel.HOT);
        Ingredient sourCream = saveAnIngredient("SRCR", "Sour Cream", Type.SAUCE, "0.95",
            EnumSet.of(DietaryTag.VEGETARIAN, DietaryTag.GLUTEN_FREE),
            EnumSet.of(Allergen.DAIRY), SpiceLevel.NONE);
        
//        UserUDT u = new UserUDT(username, fullname, phoneNumber)
        
        userRepo.save(new User("habuma", encoder.encode("password"), 
              "Craig Walls", "123 North Street", "Cross Roads", "TX", 
              "76227", "123-123-1234", "craig@habuma.com"))
          .subscribe(user -> {
          });
        
        Taco taco1 = new Taco();
        taco1.setId("TACO1");
        taco1.setName("Carnivore");
        taco1.setIngredients(Arrays.asList(flourTortilla, groundBeef, carnitas, sourCream, salsa, cheddar));
        tacoRepo.save(taco1).subscribe();

        Taco taco2 = new Taco();
        taco2.setId("TACO2");
        taco2.setName("Bovine Bounty");
        taco2.setIngredients(Arrays.asList(cornTortilla, groundBeef, cheddar, jack, sourCream));
        tacoRepo.save(taco2).subscribe();

        Taco taco3 = new Taco();
        taco3.setId("TACO3");
        taco3.setName("Veg-Out");
        taco3.setIngredients(Arrays.asList(flourTortilla, cornTortilla, tomatoes, lettuce, salsa));
        tacoRepo.save(taco3).subscribe();

      }

      //modificacion para TC-17
      private Ingredient saveAnIngredient(String id, String name, Type type, String unitPrice,
          java.util.Set<DietaryTag> tags, java.util.Set<Allergen> allergens, SpiceLevel spiceLevel) {
        Ingredient ingredient = new Ingredient(id, name, type, new BigDecimal(unitPrice),
            true, 100, 20, tags, allergens, spiceLevel);
        repo.save(ingredient).subscribe();
        return ingredient;
      }
    };
  }
  
}
