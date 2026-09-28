package tacos.web.api;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.api.dto.Tc12MigrationResponse;
import tacos.data.migration.SensitivePaymentDataMigration;

//TC-12 - Ejecucion reactiva y explicita de la migracion para laboratorio
@Profile("tc12-migration")
@RestController
@RequestMapping(path = "/api/admin/migrations/tc12", produces = "application/json")
public class Tc12SensitiveDataMigrationController {
    private final SensitivePaymentDataMigration migration;

    public Tc12SensitiveDataMigrationController(SensitivePaymentDataMigration migration) {
        this.migration = migration;
    }

    @PostMapping("/payment-data")
    public Mono<Tc12MigrationResponse> removeLegacyPaymentData() {
        return migration.removeLegacySensitiveFields().map(Tc12MigrationResponse::new);
    }
}
//Fin TC-12
