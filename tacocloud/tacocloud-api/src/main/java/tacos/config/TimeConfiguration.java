package tacos.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//TC-15 - Reloj reemplazable para reglas con vigencia
@Configuration
public class TimeConfiguration {
    @Bean
    public Clock clock(@Value("${tacocloud.time-zone:UTC}") String zone) { //modificacion para TC-20
        return Clock.system(ZoneId.of(zone));
    }
}
//Fin TC-15
