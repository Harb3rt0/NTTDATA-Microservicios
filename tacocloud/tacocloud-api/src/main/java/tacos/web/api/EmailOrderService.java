package tacos.web.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.TacoOrder;
import tacos.PaymentMethod;
import tacos.Taco;
import tacos.User;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BadRequestException;
import tacos.api.error.BusinessRuleException;
import tacos.data.IngredientRepository;
import tacos.data.PaymentMethodRepository;
import tacos.data.UserRepository;
import tacos.web.api.EmailOrder.EmailTaco;

@Service
public class EmailOrderService {

  private final IngredientController ingredientController;
  private UserRepository userRepo;
  private IngredientRepository ingredientRepo;
  private PaymentMethodRepository paymentMethodRepo;

  public EmailOrderService(UserRepository userRepo, IngredientRepository ingredientRepo,
      PaymentMethodRepository paymentMethodRepo, IngredientController ingredientController) {
    this.userRepo = userRepo;
    this.ingredientRepo = ingredientRepo;
    this.paymentMethodRepo = paymentMethodRepo;
    this.ingredientController = ingredientController;
  }

  //TC-06 - Convertir ordenes de correo sin carreras ni nulls sorpresa
  public Mono<TacoOrder> convertEmailOrderToDomainOrder(Mono<EmailOrder> emailOrder) {
    return emailOrder
      .switchIfEmpty(Mono.error(new BadRequestException(
        ApiErrorCodes.EMAIL_ORDER_REQUIRED, "Email order cannot be empty.")))
      .flatMap(eOrder -> userRepo.findByEmail(eOrder.getEmail())
        .switchIfEmpty(Mono.error(new BusinessRuleException(
          ApiErrorCodes.ORDER_USER_NOT_FOUND, "No customer exists for the supplied email.")))
        .flatMap(user -> paymentMethodRepo.findByUserId(user.getId())
          .switchIfEmpty(Mono.error(new BusinessRuleException(ApiErrorCodes.ORDER_PAYMENT_METHOD_NOT_FOUND,
          "No payment method is available for this customer.")))
          .flatMap(paymentMethod -> Mono.justOrEmpty(eOrder.getTacos())
            .switchIfEmpty(Mono.error(new BadRequestException(
              ApiErrorCodes.EMAIL_TACOS_REQUIRED, "Email order has no taco list.")))
            .flatMapMany(Flux::fromIterable)
            .concatMap(emailTaco -> Mono.justOrEmpty(emailTaco.getIngredients())
              .switchIfEmpty(Mono.error(new BadRequestException(
                ApiErrorCodes.EMAIL_TACO_INGREDIENTS_REQUIRED,
                "Taco '" + emailTaco.getName() + "' has no ingredient list.")))
              .flatMapMany(Flux::fromIterable)
              .concatMap(ingredientId -> ingredientRepo
                .findById(ingredientId)
                .switchIfEmpty(Mono.error(new BusinessRuleException(
                  ApiErrorCodes.ORDER_INGREDIENT_NOT_FOUND,
                  "Ingredient '" + ingredientId + "' is not available."))))
                .collectList()
                .map(ingredients -> {
                  Taco taco = new Taco();
                  taco.setName(emailTaco.getName());
                  taco.setIngredients(ingredients);
                  return taco;
                })
            )
            .collectList()
            .map(tacos -> {
              TacoOrder order = new TacoOrder();
              order.setUser(user);
              order.setPaymentMethodId(paymentMethod.getId()); //modificacion para TC-12
              order.setDeliveryName(user.getFullname());
              order.setDeliveryStreet(user.getStreet());
              order.setDeliveryCity(user.getCity());
              order.setDeliveryState(user.getState());
              order.setDeliveryZip(user.getZip());
              order.setPlacedAt(new Date());
              tacos.forEach(order::addTaco);
              return order;
            })
          )
        )
      );
  }
  //TC-06 - Fin

}
