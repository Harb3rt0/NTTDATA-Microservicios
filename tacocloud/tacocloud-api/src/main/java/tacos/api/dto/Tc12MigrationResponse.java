package tacos.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//TC-12 - Resultado seguro de la migracion de datos de pago
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tc12MigrationResponse {
    private long documentsModified;
}
//Fin TC-12
