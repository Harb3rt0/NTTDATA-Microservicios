package tacos.data;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.TacoOrder;
import tacos.User;

public interface OrderRepository 
         extends ReactiveCrudRepository<TacoOrder, String> {

  Flux<TacoOrder> findByUserOrderByPlacedAtDesc(
          User user, Pageable pageable);

  //TC-11 - Consulta de pedidos por la identidad autenticada
  Flux<TacoOrder> findByUserUsernameOrderByPlacedAtDesc(String username);
  //Fin TC-11

  //TC-23 - Historial paginado con orden estable y ownership en la consulta
  Flux<TacoOrder> findByUserUsernameOrderByPlacedAtDescIdAsc(String username, Pageable pageable);

  Mono<Long> countByUserUsername(String username);

  Mono<TacoOrder> findByIdAndUserUsername(String id, String username);

  Flux<TacoOrder> findAllByOrderByPlacedAtDescIdAsc(Pageable pageable);
  //Fin TC-23

}
