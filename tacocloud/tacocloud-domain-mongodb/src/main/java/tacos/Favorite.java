package tacos;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

//TC-21 - Favorito persistente por usuario y taco
@Data
@Document
@CompoundIndex(name = "favorite_user_taco_unique", def = "{'userId':1,'tacoId':1}", unique = true)
public class Favorite {
    @Id
    private String id;
    private String userId;
    private String tacoId;
    private Instant createdAt;
}
//Fin TC-21
