package tacos.messaging;

public interface OrderMessagingService {

  void sendOrder(KitchenOrderEvent order); //modificacion para TC-12
  
}
