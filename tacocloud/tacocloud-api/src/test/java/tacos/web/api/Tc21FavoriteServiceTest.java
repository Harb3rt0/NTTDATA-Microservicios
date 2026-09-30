package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.security.authentication.TestingAuthenticationToken;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Favorite;
import tacos.User;
import tacos.api.error.BadRequestException;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.TacoMapper;
import tacos.data.TacoRepository;
import tacos.data.UserRepository;

//TC-21 - Persistencia e identidad de favoritos
public class Tc21FavoriteServiceTest {
    @Test
    public void uniqueUserTacoIndexIsDeclared() {
        CompoundIndex index = Favorite.class.getAnnotation(CompoundIndex.class);
        assertThat(index.unique()).isTrue();
        assertThat(index.def()).contains("userId", "tacoId");
    }

    @Test public void rejectsNegativePageBeforeMongo() { assertInvalidPage(-1, 10); }
    @Test public void rejectsLargeSizeBeforeMongo() { assertInvalidPage(0, 51); }

    @Test
    public void missingTacoDoesNotCreateFavorite() {
        ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
        UserRepository users = mock(UserRepository.class);
        TacoRepository tacos = mock(TacoRepository.class);
        User user = new User("alice", "hash", "User", "Street", "City", "ST", "000", "555", "a@x.test");
        user.setId("u1");
        when(users.findByUsername("alice")).thenReturn(Mono.just(user));
        when(tacos.findById("missing")).thenReturn(Mono.empty());
        FavoriteService service = new FavoriteService(template, users, tacos,
            mock(TacoMapper.class), Clock.systemUTC());

        StepVerifier.create(service.add("missing", auth())).expectError(ResourceNotFoundException.class).verify();
        verifyNoInteractions(template);
    }

    private void assertInvalidPage(int page, int size) {
        ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
        FavoriteService service = new FavoriteService(template, mock(UserRepository.class),
            mock(TacoRepository.class), mock(TacoMapper.class), Clock.systemUTC());
        StepVerifier.create(service.list(page, size, auth())).expectError(BadRequestException.class).verify();
        verifyNoInteractions(template);
    }

    private TestingAuthenticationToken auth() {
        return new TestingAuthenticationToken("alice", "pw", Collections.singletonList(
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
    }
}
//Fin TC-21
