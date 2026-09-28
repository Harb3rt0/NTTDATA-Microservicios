package tacos.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import reactor.core.publisher.Mono;

//TC-12 - Verifica limpieza real e idempotente sobre Mongo embebido
@DataMongoTest
@Import(SensitivePaymentDataMigration.class)
public class SensitivePaymentDataMigrationTest {

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @Autowired
    private SensitivePaymentDataMigration migration;

    @Test
    public void shouldRemoveLegacySensitiveFieldsAndRemainIdempotent() throws Exception {
        Document order = new Document("_id", "TC12-ORDER")
            .append("ccNumber", "LEGACY_SYNTHETIC_PAN")
            .append("ccCVV", "LEGACY_SYNTHETIC_CVV")
            .append("ccExpiration", "12/39")
            .append("deliveryName", "Lab User");
        Document paymentMethod = new Document("_id", "TC12-PAYMENT")
            .append("cardNumber", "LEGACY_SYNTHETIC_PAN")
            .append("securityCode", "LEGACY_SYNTHETIC_CVV")
            .append("brand", "LAB_CARD");

        Mono<Document[]> result = mongoTemplate.remove(
                org.springframework.data.mongodb.core.query.Query.query(
                    org.springframework.data.mongodb.core.query.Criteria.where("_id")
                        .in("TC12-ORDER", "TC12-PAYMENT")), "tacoOrder")
            .then(mongoTemplate.remove(
                org.springframework.data.mongodb.core.query.Query.query(
                    org.springframework.data.mongodb.core.query.Criteria.where("_id")
                        .in("TC12-ORDER", "TC12-PAYMENT")), "paymentMethod"))
            .then(Mono.when(
                mongoTemplate.insert(order, "tacoOrder"),
                mongoTemplate.insert(paymentMethod, "paymentMethod")))
            .then(migration.removeLegacySensitiveFields())
            .then(migration.removeLegacySensitiveFields())
            .then(Mono.zip(
                mongoTemplate.findById("TC12-ORDER", Document.class, "tacoOrder"),
                mongoTemplate.findById("TC12-PAYMENT", Document.class, "paymentMethod")))
            .map(tuple -> new Document[] {tuple.getT1(), tuple.getT2()});

        Document[] documents = result.toFuture().get(10, TimeUnit.SECONDS);
        assertThat(documents[0].keySet()).doesNotContain(
            "ccNumber", "ccCVV", "ccExpiration");
        assertThat(documents[0].getString("deliveryName")).isEqualTo("Lab User");
        assertThat(documents[1].keySet()).doesNotContain(
            "cardNumber", "securityCode", "cvv", "pan");
        assertThat(documents[1].getString("brand")).isEqualTo("LAB_CARD");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
//Fin TC-12
