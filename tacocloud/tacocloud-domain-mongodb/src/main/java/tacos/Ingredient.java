package tacos;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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

  //TC-13 - Datos de catalogo e inventario persistidos
  @NotNull(message = "Ingredient unit price is required")
  @DecimalMin(value = "0.00", message = "Ingredient unit price must not be negative")
  @Digits(integer = 10, fraction = 2, message = "Ingredient unit price must have at most 2 decimals")
  private BigDecimal unitPrice = new BigDecimal("0.00");

  private boolean available;

  @Min(value = 0, message = "Ingredient stock must not be negative")
  private int stockOnHand;

  @Min(value = 0, message = "Ingredient reorder level must not be negative")
  private int reorderLevel;

  @Version
  private Long version;
  //Fin TC-13

  //TC-17 - Metadata dietaria academica del ingrediente
  private Set<DietaryTag> dietaryTags = EnumSet.noneOf(DietaryTag.class);
  private Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);
  private SpiceLevel spiceLevel = SpiceLevel.NONE;
  //Fin TC-17

  public Ingredient(String id, String name, Type type) {
    this(id, name, type, new BigDecimal("0.00"), false, 0, 0);
  }

  //TC-13 - Construye un ingrediente con catalogo inicial coherente
  public Ingredient(String id, String name, Type type, BigDecimal unitPrice,
      boolean available, int stockOnHand, int reorderLevel) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.unitPrice = unitPrice;
    this.available = available;
    this.stockOnHand = stockOnHand;
    this.reorderLevel = reorderLevel;
  }
  //Fin TC-13

  //TC-17 - Construye un ingrediente con metadata academica
  public Ingredient(String id, String name, Type type, BigDecimal unitPrice,
      boolean available, int stockOnHand, int reorderLevel, Set<DietaryTag> dietaryTags,
      Set<Allergen> allergens, SpiceLevel spiceLevel) {
    this(id, name, type, unitPrice, available, stockOnHand, reorderLevel);
    this.dietaryTags = dietaryTags == null || dietaryTags.isEmpty()
        ? EnumSet.noneOf(DietaryTag.class) : EnumSet.copyOf(dietaryTags);
    this.allergens = allergens == null || allergens.isEmpty()
        ? EnumSet.noneOf(Allergen.class) : EnumSet.copyOf(allergens);
    this.spiceLevel = spiceLevel == null ? SpiceLevel.NONE : spiceLevel;
  }
  //Fin TC-17

  public enum Type {
    WRAP, BOWL, PROTEIN, VEGGIES, CHEESE, SAUCE //modificacion para TC-18
  }

}
