package tacos.data;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import tacos.TacoOrder;
import tacos.User;

public interface OrderRepository 
         extends ReactiveCrudRepository<TacoOrder, String> {

  Flux<TacoOrder> findByUserOrderByPlacedAtDesc(
          User user, Pageable pageable);

  //TC-11 - Consulta de pedidos por la identidad autenticada
  Flux<TacoOrder> findByUserUsernameOrderByPlacedAtDesc(String username);
  //Fin TC-11

}
