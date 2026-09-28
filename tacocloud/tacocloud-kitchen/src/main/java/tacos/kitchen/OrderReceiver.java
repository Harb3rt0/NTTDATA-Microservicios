package tacos.kitchen;

import tacos.messaging.KitchenOrderEvent;

public interface OrderReceiver {

  KitchenOrderEvent receiveOrder(); //modificacion para TC-12

}
