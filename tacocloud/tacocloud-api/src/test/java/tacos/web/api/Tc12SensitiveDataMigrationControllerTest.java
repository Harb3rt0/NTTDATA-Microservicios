package tacos.web.api;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.data.migration.SensitivePaymentDataMigration;

//TC-12 - La migracion se compone sin suscripcion manual
public class Tc12SensitiveDataMigrationControllerTest {

    @Test
    public void shouldReturnResultAfterReactiveMigrationCompletes() {
        SensitivePaymentDataMigration migration = Mockito.mock(SensitivePaymentDataMigration.class);
        when(migration.removeLegacySensitiveFields()).thenReturn(Mono.just(2L));
        Tc12SensitiveDataMigrationController controller =
            new Tc12SensitiveDataMigrationController(migration);

        StepVerifier.create(controller.removeLegacyPaymentData())
            .expectNextMatches(response -> response.getDocumentsModified() == 2L)
            .verifyComplete();

        verify(migration, times(1)).removeLegacySensitiveFields();
    }
}
//Fin TC-12
