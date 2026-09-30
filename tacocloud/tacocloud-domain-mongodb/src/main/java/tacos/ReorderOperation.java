package tacos;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

//TC-24 - Registro persistente de idempotencia de recompra
@Data
@Document
public class ReorderOperation {
    @Id
    private String id;
    private String userId;
    private String originalOrderId;
    private String reorderKey;
    private String newOrderId;
    private Instant createdAt;
}
//Fin TC-24
