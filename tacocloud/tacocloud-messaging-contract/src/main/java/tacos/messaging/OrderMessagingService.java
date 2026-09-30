package tacos.messaging;

//TC-27 - Puerto unico para todos los transportes
public interface OrderMessagingService {
  void sendOrder(OrderEvent event);
}
//Fin TC-27
