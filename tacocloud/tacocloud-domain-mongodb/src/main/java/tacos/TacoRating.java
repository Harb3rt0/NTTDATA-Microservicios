package tacos;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

//TC-22 - Una calificacion vigente por usuario y taco
@Data
@Document
@CompoundIndex(name = "rating_user_taco_unique", def = "{'userId':1,'tacoId':1}", unique = true)
public class TacoRating {
    @Id
    private String id;
    private String userId;
    private String tacoId;
    private int score;
    private Instant updatedAt;
}
//Fin TC-22
