package tacos.web.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.api.dto.OrderResponse;
import tacos.api.dto.OrderSummaryResponse;
import tacos.api.dto.PagedResponse;

//TC-23 - Endpoints explicitos para historial propio
@RestController
@Validated
@RequestMapping(path = "/api/users/me/orders", produces = "application/json")
public class OrderHistoryController {
    private final OrderHistoryService historyService;

    public OrderHistoryController(OrderHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public Mono<PagedResponse<OrderSummaryResponse>> mine(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        return historyService.mine(page, size, authentication);
    }

    @GetMapping("/{orderId}")
    public Mono<OrderResponse> mineById(@PathVariable @NotBlank @Size(max = 64) String orderId,
            Authentication authentication) {
        return historyService.mineById(orderId, authentication);
    }
}
//Fin TC-23
