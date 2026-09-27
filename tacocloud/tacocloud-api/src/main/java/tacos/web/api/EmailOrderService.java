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
      .switchIfEmpty(Mono.error(new IllegalArgumentException("Email order cannot be empty")))
      .flatMap(eOrder -> userRepo.findByEmail(eOrder.getEmail())
        .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found for email: " + eOrder.getEmail())))
        .flatMap(user -> paymentMethodRepo.findByUserId(user.getId())
          .switchIfEmpty(Mono.error(new IllegalArgumentException("Payment method not found for user: " + user.getId())))
          .flatMap(paymentMethod -> Mono.justOrEmpty(eOrder.getTacos())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Email order has no taco list")))
            .flatMapMany(Flux::fromIterable)
            .concatMap(emailTaco -> Mono.justOrEmpty(emailTaco.getIngredients())
              .switchIfEmpty(Mono.error(new IllegalArgumentException("Taco '" + emailTaco.getName() + "' has no ingredient list")))
              .flatMapMany(Flux::fromIterable)
              .concatMap(ingredientId -> ingredientRepo
                .findById(ingredientId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ingredient not found for id: " + ingredientId))))
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
              order.setCcNumber(paymentMethod.getCcNumber());
              order.setCcCVV(paymentMethod.getCcCVV());
              order.setCcExpiration(paymentMethod.getCcExpiration());
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
