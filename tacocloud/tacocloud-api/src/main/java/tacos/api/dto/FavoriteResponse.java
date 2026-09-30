package tacos.api.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

//TC-21 - Respuesta segura de favorito
@Data
@AllArgsConstructor
public class FavoriteResponse {
    private String tacoId;
    private Instant createdAt;
    private TacoResponse taco;
}
//Fin TC-21
