package tacos.web.api;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.api.dto.OrderResponse;
import tacos.api.dto.OrderStatusRequest;
import tacos.api.mapper.OrderMapper;

//TC-25 - Endpoints explicitos de estado y cancelacion
@RestController
@Validated
@RequestMapping(path = "/api/orders", produces = "application/json")
public class OrderWorkflowController {
  private final OrderWorkflowService workflowService;
  private final OrderMapper orderMapper;

  public OrderWorkflowController(OrderWorkflowService workflowService, OrderMapper orderMapper) {
    this.workflowService = workflowService;
    this.orderMapper = orderMapper;
  }

  @PatchMapping(path = "/{orderId}/status", consumes = "application/json")
  public Mono<OrderResponse> transition(
      @PathVariable @NotBlank @Size(max = 64) String orderId,
      @Valid @RequestBody OrderStatusRequest request, Authentication authentication) {
    return workflowService.transition(orderId, request.getStatus(), request.getReason(), authentication)
        .map(orderMapper::toResponse);
  }

  @PostMapping("/{orderId}/cancel")
  public Mono<OrderResponse> cancel(
      @PathVariable @NotBlank @Size(max = 64) String orderId, Authentication authentication) {
    return workflowService.cancel(orderId, authentication).map(orderMapper::toResponse);
  }
}
//Fin TC-25
