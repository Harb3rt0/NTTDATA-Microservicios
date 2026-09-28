package tacos.data;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import reactor.core.publisher.Mono;
import tacos.User;

@RepositoryRestResource(exported = false) //modificacion para TC-10
public interface UserRepository extends ReactiveCrudRepository<User, String> {

  Mono<User> findByUsername(String username);
  
  Mono<User> findByEmail(String email);
  
}
