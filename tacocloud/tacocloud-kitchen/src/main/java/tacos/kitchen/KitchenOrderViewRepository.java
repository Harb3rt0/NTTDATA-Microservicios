package tacos.kitchen;

import org.springframework.data.mongodb.repository.MongoRepository;

//TC-30 - Persistencia de la vista de cocina
public interface KitchenOrderViewRepository extends MongoRepository<KitchenOrderView, String> {
}
//Fin TC-30
