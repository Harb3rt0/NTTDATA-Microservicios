package tacos.kitchen;

import org.springframework.data.mongodb.repository.MongoRepository;

//TC-30 - Consulta idempotente por eventId
public interface ProcessedEventRepository extends MongoRepository<ProcessedEvent, String> {
  boolean existsByEventId(String eventId);
}
//Fin TC-30
