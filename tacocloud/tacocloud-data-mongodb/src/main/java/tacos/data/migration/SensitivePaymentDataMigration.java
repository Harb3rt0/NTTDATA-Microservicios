package tacos.data.migration;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

//TC-12 - Migracion idempotente de campos de pago heredados
@Component
public class SensitivePaymentDataMigration {
    private static final String ORDER_COLLECTION = "tacoOrder";
    private static final String PAYMENT_METHOD_COLLECTION = "paymentMethod";

    private final ReactiveMongoTemplate mongoTemplate;

    public SensitivePaymentDataMigration(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<Long> removeLegacySensitiveFields() {
        return Mono.zip(removeFieldsFrom(ORDER_COLLECTION), removeFieldsFrom(PAYMENT_METHOD_COLLECTION))
            .map(results -> results.getT1() + results.getT2());
    }

    private Mono<Long> removeFieldsFrom(String collection) {
        Update update = new Update()
            .unset("ccNumber")
            .unset("ccCVV")
            .unset("ccExpiration")
            .unset("cardNumber")
            .unset("cvv")
            .unset("securityCode")
            .unset("pan");
        return mongoTemplate.updateMulti(new Query(), update, collection)
            .map(result -> result.getModifiedCount());
    }
}
//Fin TC-12
