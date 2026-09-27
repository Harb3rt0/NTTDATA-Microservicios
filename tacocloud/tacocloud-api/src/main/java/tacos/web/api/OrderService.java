package tacos.web.api;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.TacoOrder;
import tacos.data.OrderRepository;
import tacos.messaging.OrderMessagingService;

@Service 
public class OrderService {
    private final OrderRepository repo;
    private final OrderMessagingService orderMessages;

    public OrderService(OrderRepository repo, OrderMessagingService orderMessages) {
        this.repo = repo;
        this.orderMessages = orderMessages;
    }

    //TC-07 - Una sola suscripcion para guarar y publicar
    public Mono<TacoOrder> saveAndPublish(TacoOrder order) {
        return repo.save(order)
            .flatMap(savedOrder ->
                Mono.fromRunnable(() -> orderMessages.sendOrder(savedOrder))
                .thenReturn(savedOrder)
            );
    }
    //TC-07 - Fin
}
