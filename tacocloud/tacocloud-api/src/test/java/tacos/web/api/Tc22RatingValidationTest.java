package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;

import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import reactor.test.StepVerifier;
import tacos.api.dto.RatingRequest;
import tacos.api.error.BadRequestException;
import tacos.api.mapper.TacoMapper;
import tacos.data.TacoRepository;
import tacos.data.UserRepository;

//TC-22 - Rango de voto y limite seguro
public class Tc22RatingValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test public void rejectsMissingScore() { assertInvalid(null); }
    @Test public void rejectsScoreBelowOne() { assertInvalid(0); }
    @Test public void rejectsScoreAboveFive() { assertInvalid(6); }
    @Test public void acceptsScoreInRange() { assertThat(violations(5)).isZero(); }

    @Test
    public void rejectsInvalidTopLimitBeforeMongo() {
        ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
        TacoRatingService service = new TacoRatingService(template, mock(UserRepository.class),
            mock(TacoRepository.class), mock(TacoMapper.class), Clock.systemUTC(), 1);
        StepVerifier.create(service.top(0)).expectError(BadRequestException.class).verify();
        verifyNoInteractions(template);
    }

    private void assertInvalid(Integer score) { assertThat(violations(score)).isPositive(); }
    private int violations(Integer score) {
        RatingRequest request = new RatingRequest(); request.setScore(score);
        return validator.validate(request).size();
    }
}
//Fin TC-22
