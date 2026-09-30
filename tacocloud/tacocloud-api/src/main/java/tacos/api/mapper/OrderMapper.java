package tacos.api.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tacos.OrderLine;
import tacos.TacoOrder;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.dto.OrderLineResponse;
import tacos.api.dto.OrderPatchRequest;
import tacos.api.dto.OrderQuoteResponse;
import tacos.api.dto.OrderResponse;
import tacos.api.dto.OrderStatusHistoryResponse;
import tacos.api.dto.OrderUpdateRequest;

@Component 
public class OrderMapper {
    private final TacoMapper tacoMapper;

    public OrderMapper(TacoMapper tacoMapper) {
        this.tacoMapper = tacoMapper;
    }

    public TacoOrder toEntity(OrderCreateRequest request, List<OrderLine> items,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total,
            String couponCode, String currency) { //modificacion para TC-15
        TacoOrder order = new TacoOrder();
        order.setDeliveryName(request.getDeliveryName());
        order.setDeliveryStreet(request.getDeliveryStreet());
        order.setDeliveryCity(request.getDeliveryCity());
        order.setDeliveryState(request.getDeliveryState());
        order.setDeliveryZip(request.getDeliveryZip());
        order.setPaymentMethodId(request.getPaymentMethodId()); //modificacion para TC-12
        order.setItems(items); //modificacion para TC-14
        order.setSubtotalBeforeDiscount(subtotal);
        order.setDiscountAmount(discount);
        order.setTotal(total);
        order.setCurrency(currency);
        order.setCouponCode(couponCode);
        return order;
    }

    public void updateEntity(OrderUpdateRequest request, TacoOrder order, List<OrderLine> items,
            BigDecimal total, String currency) { //modificacion para TC-14
        order.setDeliveryName(request.getDeliveryName());
        order.setDeliveryStreet(request.getDeliveryStreet());
        order.setDeliveryCity(request.getDeliveryCity());
        order.setDeliveryState(request.getDeliveryState());
        order.setDeliveryZip(request.getDeliveryZip());
        order.setItems(items); //modificacion para TC-14
        order.setTotal(total);
        order.setCurrency(currency);
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
        response.setItems(order.getItems().stream()
            .map(this::toLineResponse)
            .collect(Collectors.toList())
        ); //modificacion para TC-14
        response.setSubtotalBeforeDiscount(order.getSubtotalBeforeDiscount()); //modificacion para TC-15
        response.setDiscountAmount(order.getDiscountAmount());
        response.setTotal(order.getTotal());
        response.setCurrency(order.getCurrency());
        response.setCouponCode(order.getCouponCode());
        //modificacion para TC-25
        response.setStatus(order.getStatus());
        response.setStationId(order.getStationId());
        response.setStatusHistory(order.getStatusHistory().stream().map(history -> {
            OrderStatusHistoryResponse item = new OrderStatusHistoryResponse();
            item.setFrom(history.getFrom());
            item.setTo(history.getTo());
            item.setActor(history.getActor());
            item.setOccurredAt(history.getOccurredAt());
            item.setOrigin(history.getOrigin());
            item.setReason(history.getReason());
            return item;
        }).collect(Collectors.toList()));

        return response;
    }

    //TC-15 - Mapea una cotizacion sin entidad persistida
    public OrderQuoteResponse toQuoteResponse(List<OrderLine> items, BigDecimal subtotal,
            BigDecimal discount, BigDecimal total, String couponCode, String currency) {
        OrderQuoteResponse response = new OrderQuoteResponse();
        response.setItems(items.stream().map(this::toLineResponse).collect(Collectors.toList()));
        response.setSubtotalBeforeDiscount(subtotal);
        response.setDiscountAmount(discount);
        response.setTotal(total);
        response.setCouponCode(couponCode);
        response.setCurrency(currency);
        return response;
    }
    //Fin TC-15

    //TC-14 - Mapea el snapshot persistido sin recalcular precios
    private OrderLineResponse toLineResponse(OrderLine item) {
        OrderLineResponse response = new OrderLineResponse();
        response.setTaco(tacoMapper.toResponse(item.getTaco()));
        response.setQuantity(item.getQuantity());
        response.setUnitPriceAtPurchase(item.getUnitPriceAtPurchase());
        response.setSubtotal(item.getSubtotal());
        return response;
    }
    //Fin TC-14
}
