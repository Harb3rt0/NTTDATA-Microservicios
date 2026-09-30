package tacos.web.api;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.TacoOrder;
import tacos.User;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.dto.OrderPatchRequest;
import tacos.api.dto.OrderQuoteRequest;
import tacos.api.dto.OrderQuoteResponse;
import tacos.api.dto.OrderUpdateRequest;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.KitchenOrderEventMapper;
import tacos.api.mapper.OrderMapper;
import tacos.coupon.CouponApplication;
import tacos.coupon.CouponService;
import tacos.data.OrderRepository;
import tacos.data.PaymentMethodRepository;
import tacos.data.UserRepository;
import tacos.messaging.OrderMessagingService;

@Service 
public class OrderService {
    private final OrderRepository repo;
    private final OrderMessagingService orderMessages;
    private final OrderMapper orderMapper;
    private final UserRepository userRepo;
    private final PaymentMethodRepository paymentMethodRepo;
    private final KitchenOrderEventMapper kitchenOrderEventMapper;
    private final OrderPricingService pricingService;
    private final CouponService couponService;
    private final TacoDesignService tacoDesignService;
    private final InventoryService inventoryService;

    //modificacion para TC-08
    //modificacion para TC-14
    public OrderService(OrderRepository repo, OrderMessagingService orderMessages, OrderMapper orderMapper,
            UserRepository userRepo, PaymentMethodRepository paymentMethodRepo,
            KitchenOrderEventMapper kitchenOrderEventMapper, OrderPricingService pricingService,
            CouponService couponService, TacoDesignService tacoDesignService,
            InventoryService inventoryService) { //modificacion para TC-16
        this.repo = repo;
        this.orderMessages = orderMessages;
        this.orderMapper = orderMapper;
        this.userRepo = userRepo; //modificacion para TC-11
        this.paymentMethodRepo = paymentMethodRepo; //modificacion para TC-12
        this.kitchenOrderEventMapper = kitchenOrderEventMapper; //modificacion para TC-12
        this.pricingService = pricingService; //modificacion para TC-14
        this.couponService = couponService;
        this.tacoDesignService = tacoDesignService;
        this.inventoryService = inventoryService;
    }

    //TC-07 - Una sola suscripcion para guarar y publicar
    public Mono<TacoOrder> saveAndPublish(TacoOrder order) {
        return repo.save(order)
            .flatMap(this::publish); //modificacion para TC-16
    }
    //TC-07 - Fin

    //TC-08 - Separar DTOs de entrada, respuesta y persistencia
    public Mono<TacoOrder> createOrder(OrderCreateRequest request, Authentication authentication) {
        //modificacion para TC-24
        return prepareOrder(request, authentication)
            .flatMap(order -> reserveAndPersist(order, UUID.randomUUID().toString()));
    }

    //TC-24 - Prepara y valida sin reservar para comparar una recompra
    public Mono<TacoOrder> prepareOrder(OrderCreateRequest request, Authentication authentication) {
        return Mono.defer(() -> authenticatedUser(authentication)
            .flatMap(user -> ownedPaymentMethod(request.getPaymentMethodId(), user) //modificacion para TC-12
                .flatMap(paymentMethod -> priceAndDiscount(request.getItems(), request.getCouponCode())
                    .map(priced -> {
                        CouponApplication coupon = priced.getCoupon();
                        TacoOrder order = orderMapper.toEntity(request, priced.getItems(),
                            coupon.getSubtotalBeforeDiscount(), coupon.getDiscountAmount(),
                            coupon.getTotal(), coupon.getCouponCode(), pricingService.getCurrency());
                        order.setUser(user);
                        order.setPaymentMethodId(paymentMethod.getId());
                        return order;
                    }))));
    }

    public Mono<TacoOrder> createPreparedOrder(TacoOrder order, String reservationKey) {
        return reserveAndPersist(order, reservationKey);
    }
    //Fin TC-24

    //TC-16 - Reserva despues de validar y compensa si falla el guardado
    private Mono<TacoOrder> reserveAndPersist(TacoOrder order, String reservationKey) {
        return inventoryService.reserve(reservationKey, order.getItems())
            .flatMap(reservation -> {
                order.setInventoryReservationKey(reservationKey);
                return repo.save(order)
                    .onErrorResume(error -> inventoryService.release(reservationKey)
                        .then(Mono.error(error)))
                    .flatMap(saved -> inventoryService.confirm(reservationKey, saved.getId())
                        .then(publish(saved)));
            });
    }
    //Fin TC-16

    //TC-16 - Publica solo despues de persistir y conservar la reserva
    private Mono<TacoOrder> publish(TacoOrder savedOrder) {
        return Mono.fromRunnable(() -> orderMessages.sendOrder(
            kitchenOrderEventMapper.toEvent(savedOrder)))
            .thenReturn(savedOrder);
    }
    //Fin TC-16

    //TC-15 - Cotiza sin guardar, publicar ni reservar
    public Mono<OrderQuoteResponse> quoteOrder(OrderQuoteRequest request, Authentication authentication) {
        return authenticatedUser(authentication)
            .then(priceAndDiscount(request.getItems(), request.getCouponCode()))
            .map(priced -> orderMapper.toQuoteResponse(priced.getItems(),
                priced.getCoupon().getSubtotalBeforeDiscount(), priced.getCoupon().getDiscountAmount(),
                priced.getCoupon().getTotal(), priced.getCoupon().getCouponCode(),
                pricingService.getCurrency()));
    }
    //Fin TC-15

    //TC-15 - Reutiliza pricing y cupon en create y quote
    private Mono<PricedOrder> priceAndDiscount(java.util.List<tacos.api.dto.OrderLineCreateRequest> requests,
            String couponCode) {
        return Mono.defer(() -> {
            pricingService.validateQuantities(requests); //modificacion para TC-18
            return tacoDesignService.resolveValidateAndPrice(requests).collectList()
                .map(items -> new PricedOrder(items,
                    couponService.apply(couponCode, pricingService.calculateTotal(items))));
        });
    }
    //Fin TC-15

    //TC-15 - Contexto economico interno
    private static class PricedOrder {
        private final java.util.List<tacos.OrderLine> items;
        private final CouponApplication coupon;

        PricedOrder(java.util.List<tacos.OrderLine> items, CouponApplication coupon) {
            this.items = items;
            this.coupon = coupon;
        }

        java.util.List<tacos.OrderLine> getItems() {
            return items;
        }

        CouponApplication getCoupon() {
            return coupon;
        }
    }
    //Fin TC-15

    public Mono<TacoOrder> updateOrder(String orderId, OrderUpdateRequest request, Authentication authentication) {
        //modificacion para TC-11
        return accessibleOrder(orderId, authentication)
        .flatMap(existingOrder ->
            pricingService.priceItems(request.getItems())
                .collectList()
                .map(items -> { orderMapper.updateEntity(request, existingOrder, items,
                        pricingService.calculateTotal(items), pricingService.getCurrency());
                    return existingOrder;
                })
                .flatMap(repo::save)
        );
    }
    //TC-08 - fin

    //TC-11 - Lista pedidos propios o todos para auditoría ADMIN
    public Flux<TacoOrder> findOrders(Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return repo.findAll();
        }
        if (!hasRole(authentication, "ROLE_USER")) {
            return Flux.error(new AccessDeniedException("Order access is not allowed."));
        }
        return repo.findByUserUsernameOrderByPlacedAtDesc(authentication.getName());
    }
    //Fin TC-11

    //TC-11 - PATCH con ownership validado en servicio
    public Mono<TacoOrder> patchOrder(String orderId, OrderPatchRequest patch, Authentication authentication) {
        return accessibleOrder(orderId, authentication)
            .map(order -> {
                orderMapper.patchEntity(patch, order);
                return order;
            })
            .flatMap(repo::save);
    }
    //Fin TC-11

    //TC-11 - DELETE con ownership validado en servicio
    public Mono<Void> deleteOrder(String orderId, Authentication authentication) {
        return accessibleOrder(orderId, authentication)
            .flatMap(order -> inventoryService.release(order.getInventoryReservationKey())
                .then(repo.deleteById(order.getId()))); //modificacion para TC-16
    }
    //Fin TC-11

    //TC-11 - Resuelve el usuario desde la identidad autenticada
    private Mono<User> authenticatedUser(Authentication authentication) {
        if (!hasRole(authentication, "ROLE_USER") && !hasRole(authentication, "ROLE_ADMIN")) {
            return Mono.error(new AccessDeniedException("Order access is not allowed."));
        }
        return userRepo.findByUsername(authentication.getName())
            .switchIfEmpty(Mono.error(new AccessDeniedException("Authenticated user is unavailable.")));
    }
    //Fin TC-11

    //TC-12 - Valida que el metodo de pago exista y pertenezca al usuario autenticado
    private Mono<tacos.PaymentMethod> ownedPaymentMethod(String paymentMethodId, User user) {
        return paymentMethodRepo.findById(paymentMethodId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                ApiErrorCodes.PAYMENT_METHOD_NOT_FOUND, "Payment method was not found.")))
            .flatMap(paymentMethod -> {
                if (paymentMethod.getUser() != null
                        && user.getUsername().equals(paymentMethod.getUser().getUsername())) {
                    return Mono.just(paymentMethod);
                }
                return Mono.error(new AccessDeniedException(
                    "The payment method belongs to another user."));
            });
    }
    //Fin TC-12

    //TC-11 - Autoriza una orden usando su propietario persistido
    private Mono<TacoOrder> accessibleOrder(String orderId, Authentication authentication) {
        return repo.findById(orderId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                ApiErrorCodes.ORDER_NOT_FOUND, "Order was not found.")))
            .flatMap(order -> {
                if (hasRole(authentication, "ROLE_ADMIN")) {
                    return Mono.just(order);
                }
                if (hasRole(authentication, "ROLE_USER") && order.getUser() != null
                        && authentication.getName().equals(order.getUser().getUsername())) {
                    return Mono.just(order);
                }
                return Mono.error(new AccessDeniedException("The order belongs to another user."));
            });
    }
    //Fin TC-11

    //TC-11 - Comprobación compacta de autoridades
    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.isAuthenticated()
            && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
    //Fin TC-11
}
