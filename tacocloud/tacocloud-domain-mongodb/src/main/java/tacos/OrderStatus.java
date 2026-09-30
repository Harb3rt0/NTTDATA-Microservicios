package tacos;

//TC-25 - Estados persistidos del ciclo de vida de una orden
public enum OrderStatus {
  CREATED,
  ACCEPTED,
  PREPARING,
  READY,
  OUT_FOR_DELIVERY,
  DELIVERED,
  CANCELLED
}
//Fin TC-25
