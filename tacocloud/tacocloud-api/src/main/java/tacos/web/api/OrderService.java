package tacos.web.api;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.TacoOrder;
import tacos.User;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.dto.OrderPatchRequest;
import tacos.api.dto.OrderUpdateRequest;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.KitchenOrderEventMapper;
import tacos.api.mapper.OrderMapper;
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

    //modificacion para TC-08
    //modificacion para TC-14
    public OrderService(OrderRepository repo, OrderMessagingService orderMessages, OrderMapper orderMapper,
            UserRepository userRepo, PaymentMethodRepository paymentMethodRepo,
            KitchenOrderEventMapper kitchenOrderEventMapper, OrderPricingService pricingService) {
        this.repo = repo;
        this.orderMessages = orderMessages;
        this.orderMapper = orderMapper;
        this.userRepo = userRepo; //modificacion para TC-11
        this.paymentMethodRepo = paymentMethodRepo; //modificacion para TC-12
        this.kitchenOrderEventMapper = kitchenOrderEventMapper; //modificacion para TC-12
        this.pricingService = pricingService; //modificacion para TC-14
    }

    //TC-07 - Una sola suscripcion para guarar y publicar
    public Mono<TacoOrder> saveAndPublish(TacoOrder order) {
        return repo.save(order)
            .flatMap(savedOrder ->
                Mono.fromRunnable(() -> orderMessages.sendOrder(
                    kitchenOrderEventMapper.toEvent(savedOrder))) //modificacion para TC-12
                .thenReturn(savedOrder)
            );
    }
    //TC-07 - Fin

    //TC-08 - Separar DTOs de entrada, respuesta y persistencia
    public Mono<TacoOrder> createOrder(OrderCreateRequest request, Authentication authentication) {
        //modificacion para TC-11
        return authenticatedUser(authentication)
            .flatMap(user -> ownedPaymentMethod(request.getPaymentMethodId(), user) //modificacion para TC-12
                .flatMap(paymentMethod -> pricingService.priceItems(request.getItems())
                    .collectList()
                    .map(items -> {
                        TacoOrder order = orderMapper.toEntity(request, items,
                            pricingService.calculateTotal(items), pricingService.getCurrency());
                        order.setUser(user);
                        order.setPaymentMethodId(paymentMethod.getId());
                        return order;
                    })))
            .flatMap(this::saveAndPublish);
    }

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
            .flatMap(order -> repo.deleteById(order.getId()));
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
