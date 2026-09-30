package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tacos.Taco;
import tacos.api.dto.TacoResponse;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.TacoMapper;
import tacos.physics.TacoDesignValidator;

//TC-20 - Seleccion diaria determinista
public class Tc20TacoOfTheDayServiceTest {
    @Test
    public void sameDateReturnsSameTaco() {
        ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
        TacoDesignValidator validator = mock(TacoDesignValidator.class);
        TacoMapper mapper = mock(TacoMapper.class);
        Taco a = taco("a"); Taco b = taco("b");
        when(template.find(any(), eq(Taco.class))).thenReturn(Flux.just(b, a));
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(mapper.toResponse(any())).thenAnswer(call -> {
            TacoResponse response = new TacoResponse(); response.setId(((Taco) call.getArgument(0)).getId());
            return response;
        });
        TacoOfTheDayService service = new TacoOfTheDayService(template, validator, mapper,
            Clock.fixed(Instant.parse("2026-09-29T12:00:00Z"), ZoneOffset.UTC));

        StepVerifier.create(service.getToday().zipWith(service.getToday()))
            .assertNext(pair -> assertThat(pair.getT1().getTaco().getId())
                .isEqualTo(pair.getT2().getTaco().getId())).verifyComplete();
    }

    @Test
    public void noCandidateReturnsNotFound() {
        ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
        when(template.find(any(), eq(Taco.class))).thenReturn(Flux.empty());
        TacoOfTheDayService service = new TacoOfTheDayService(template,
            mock(TacoDesignValidator.class), mock(TacoMapper.class), Clock.systemUTC());
        StepVerifier.create(service.getToday()).expectError(ResourceNotFoundException.class).verify();
    }

    private Taco taco(String id) {
        Taco taco = new Taco(); taco.setId(id); taco.setName("Taco " + id);
        taco.setIngredients(Arrays.asList()); return taco;
    }
}
//Fin TC-20
