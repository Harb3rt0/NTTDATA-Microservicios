package tacos.data;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.User;

//TC-10 - Crear indices unicos unicamente sobre la coleccion de usuarios
@Service
public class UserIndexService {
  private final Mono<Void> indexesReady;

  public UserIndexService(ReactiveMongoTemplate mongoTemplate) {
    this.indexesReady = mongoTemplate.indexOps(User.class)
        .ensureIndex(new Index().on("username", Direction.ASC).named("uk_user_username").unique())
        .then(mongoTemplate.indexOps(User.class)
            .ensureIndex(new Index().on("email", Direction.ASC).named("uk_user_email").unique()))
        .then()
        .cache();
  }

  public Mono<Void> ensureUniqueIndexes() {
    return indexesReady;
  }
}
//Fin TC-10
