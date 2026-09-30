package tacos.web.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.TacoOrder;
import tacos.api.dto.OrderResponse;
import tacos.api.dto.OrderSummaryResponse;
import tacos.api.dto.PagedResponse;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BadRequestException;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.OrderMapper;
import tacos.data.OrderRepository;

//TC-23 - Historial propio y auditoria administrativa separados
@Service
public class OrderHistoryService {
    private final OrderRepository orderRepo;
    private final OrderMapper orderMapper;

    public OrderHistoryService(OrderRepository orderRepo, OrderMapper orderMapper) {
        this.orderRepo = orderRepo;
        this.orderMapper = orderMapper;
    }

    public Mono<PagedResponse<OrderSummaryResponse>> mine(int page, int size,
            Authentication authentication) {
        validatePage(page, size);
        String username = username(authentication);
        Flux<TacoOrder> orders = orderRepo.findByUserUsernameOrderByPlacedAtDescIdAsc(
            username, PageRequest.of(page, size));
        return page(orders, orderRepo.countByUserUsername(username), page, size);
    }

    public Mono<OrderResponse> mineById(String orderId, Authentication authentication) {
        return orderRepo.findByIdAndUserUsername(orderId, username(authentication))
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                ApiErrorCodes.ORDER_NOT_FOUND, "Order was not found.")))
            .map(orderMapper::toResponse);
    }

    public Mono<PagedResponse<OrderSummaryResponse>> admin(int page, int size) {
        validatePage(page, size);
        return page(orderRepo.findAllByOrderByPlacedAtDescIdAsc(PageRequest.of(page, size)),
            orderRepo.count(), page, size);
    }

    public Mono<OrderResponse> adminById(String orderId) {
        return orderRepo.findById(orderId).switchIfEmpty(Mono.error(new ResourceNotFoundException(
            ApiErrorCodes.ORDER_NOT_FOUND, "Order was not found."))).map(orderMapper::toResponse);
    }

    private Mono<PagedResponse<OrderSummaryResponse>> page(Flux<TacoOrder> orders, Mono<Long> count,
            int page, int size) {
        return Mono.zip(orders.map(this::summary).collectList(), count).map(result -> {
            int pages = result.getT2() == 0 ? 0 : (int) ((result.getT2() + size - 1) / size);
            return new PagedResponse<>(result.getT1(), page, size, result.getT2(), pages,
                page + 1 < pages);
        });
    }

    private OrderSummaryResponse summary(TacoOrder order) {
        int itemCount = order.getItems() == null ? 0
            : order.getItems().stream().mapToInt(item -> item.getQuantity()).sum();
        return new OrderSummaryResponse(order.getId(), order.getPlacedAt(), itemCount,
            order.getTotal(), order.getCurrency());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BadRequestException(ApiErrorCodes.VALIDATION_FAILED,
                "Page must be non-negative and size must be between 1 and 50.");
        }
    }

    private String username(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required.");
        }
        return authentication.getName();
    }
}
//Fin TC-23
