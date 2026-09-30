package tacos.data;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import tacos.OutboxEvent;

//TC-29 - Repositorio reactivo del outbox
public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, String> {
}
//Fin TC-29
