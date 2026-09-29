package tacos;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

//TC-16 - Documento idempotente de reserva
@Data
@Document
public class InventoryReservation {
    @Id
    private String id;

    @Indexed(unique = true)
    private String reservationKey;

    private String orderId;
    private ReservationStatus status;
    private List<ReservedIngredient> items = new ArrayList<>();
}
//Fin TC-16
