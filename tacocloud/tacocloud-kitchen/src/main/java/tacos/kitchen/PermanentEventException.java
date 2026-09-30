package tacos.kitchen;

//TC-30 - Error no reintentable del contrato
public class PermanentEventException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public PermanentEventException(String message) {
    super(message);
  }
}
//Fin TC-30
