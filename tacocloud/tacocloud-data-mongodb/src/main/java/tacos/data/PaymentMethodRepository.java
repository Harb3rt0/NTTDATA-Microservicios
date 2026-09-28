package tacos.data;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import reactor.core.publisher.Mono;
import tacos.PaymentMethod;

@RepositoryRestResource(exported = false) //modificacion para TC-12
public interface PaymentMethodRepository 
         extends ReactiveCrudRepository<PaymentMethod, String> {
  Mono<PaymentMethod> findByUserId(String userId);
}
