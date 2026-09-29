package tacos.api.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

//TC-17 - El cliente no controla la clasificacion derivada
public class TacoCreateRequestTest {
    @Test
    public void shouldIgnoreClientSuppliedClassification() throws Exception {
        String json = "{\"name\":\"Safe taco\",\"ingredientIds\":[\"A\",\"B\"],"
            + "\"dietaryTags\":[\"VEGAN\"],\"allergens\":[],\"spiceLevel\":\"NONE\"}";

        TacoCreateRequest request = new ObjectMapper().readValue(json, TacoCreateRequest.class);

        assertFalse(java.util.Arrays.stream(TacoCreateRequest.class.getDeclaredFields())
            .anyMatch(field -> field.getName().equals("dietaryTags")
                || field.getName().equals("allergens") || field.getName().equals("spiceLevel")));
    }
}
//Fin TC-17
