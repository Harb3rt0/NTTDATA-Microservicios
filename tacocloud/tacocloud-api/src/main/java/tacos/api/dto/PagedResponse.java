package tacos.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

//TC-19 - Respuesta paginada comun para catalogos
@Data
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}
//Fin TC-19
