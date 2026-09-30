package tacos.web.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

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

//TC-23 - Auditoria global reservada a ADMIN
@RestController
@Validated
@RequestMapping(path = "/api/admin/orders", produces = "application/json")
public class AdminOrderHistoryController {
    private final OrderHistoryService historyService;

    public AdminOrderHistoryController(OrderHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public Mono<PagedResponse<OrderSummaryResponse>> all(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return historyService.admin(page, size);
    }

    @GetMapping("/{orderId}")
    public Mono<OrderResponse> byId(@PathVariable @NotBlank @Size(max = 64) String orderId) {
        return historyService.adminById(orderId);
    }
}
//Fin TC-23
