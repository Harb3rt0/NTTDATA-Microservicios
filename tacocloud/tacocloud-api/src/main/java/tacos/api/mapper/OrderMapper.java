package tacos.api.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tacos.Taco;
import tacos.TacoOrder;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.dto.OrderPatchRequest;
import tacos.api.dto.OrderResponse;
import tacos.api.dto.OrderUpdateRequest;

@Component 
public class OrderMapper {
    private final TacoMapper tacoMapper;

    public OrderMapper(TacoMapper tacoMapper) {
        this.tacoMapper = tacoMapper;
    }

    public TacoOrder toEntity(OrderCreateRequest request, List<Taco> tacos) {
        TacoOrder order = new TacoOrder();
        order.setDeliveryName(request.getDeliveryName());
        order.setDeliveryStreet(request.getDeliveryStreet());
        order.setDeliveryCity(request.getDeliveryCity());
        order.setDeliveryState(request.getDeliveryState());
        order.setDeliveryZip(request.getDeliveryZip());
        order.setPaymentMethodId(request.getPaymentMethodId()); //modificacion para TC-12
        order.setTacos(tacos);
        return order;
    }

    public void updateEntity(OrderUpdateRequest request, TacoOrder order, List<Taco> tacos) {
        order.setDeliveryName(request.getDeliveryName());
        order.setDeliveryStreet(request.getDeliveryStreet());
        order.setDeliveryCity(request.getDeliveryCity());
        order.setDeliveryState(request.getDeliveryState());
        order.setDeliveryZip(request.getDeliveryZip());
        order.setTacos(tacos);
    }

    public void patchEntity(OrderPatchRequest patch, TacoOrder order) {
        if (patch.getDeliveryName() != null) {
            order.setDeliveryName(patch.getDeliveryName());
        }

        if (patch.getDeliveryStreet() != null) {
            order.setDeliveryStreet(patch.getDeliveryStreet());
        }

        if (patch.getDeliveryCity() != null) {
            order.setDeliveryCity(patch.getDeliveryCity());
        }

        if (patch.getDeliveryState() != null) {
            order.setDeliveryState(patch.getDeliveryState());
        }

        if (patch.getDeliveryZip() != null) {
            order.setDeliveryZip(patch.getDeliveryZip());
        }
    }

    public OrderResponse toResponse(TacoOrder order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setPlacedAt(order.getPlacedAt());
        response.setDeliveryName(order.getDeliveryName());
        response.setDeliveryStreet(order.getDeliveryStreet());
        response.setDeliveryCity(order.getDeliveryCity());
        response.setDeliveryState(order.getDeliveryState());
        response.setDeliveryZip(order.getDeliveryZip());
        response.setTacos(order.getTacos().stream()
            .map(tacoMapper::toResponse)
            .collect(Collectors.toList())
        );

        return response;
    }
}
