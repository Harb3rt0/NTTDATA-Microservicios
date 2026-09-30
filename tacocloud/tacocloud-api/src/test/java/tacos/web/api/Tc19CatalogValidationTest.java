package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import tacos.api.dto.TacoSearchCriteria;
import tacos.api.error.BadRequestException;
import tacos.api.mapper.TacoMapper;

//TC-19 - Validacion focalizada del catalogo
public class Tc19CatalogValidationTest {
    private ReactiveMongoTemplate template;
    private TacoCatalogService service;

    @BeforeEach
    public void setUp() {
        template = mock(ReactiveMongoTemplate.class);
        service = new TacoCatalogService(template, mock(TacoMapper.class));
    }

    @Test public void rejectsNegativePageBeforeMongo() { assertInvalid(-1, 12, "createdAt", "desc"); }
    @Test public void rejectsOversizedPageBeforeMongo() { assertInvalid(0, 51, "createdAt", "desc"); }
    @Test public void rejectsUnknownSortBeforeMongo() { assertInvalid(0, 12, "total", "desc"); }
    @Test public void rejectsUnknownDirectionBeforeMongo() { assertInvalid(0, 12, "name", "sideways"); }

    private void assertInvalid(int page, int size, String sort, String direction) {
        TacoSearchCriteria criteria = new TacoSearchCriteria();
        criteria.setPage(page); criteria.setSize(size); criteria.setSort(sort); criteria.setDirection(direction);
        assertThatThrownBy(() -> service.search(criteria)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(template);
    }
}
//Fin TC-19
