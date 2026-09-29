package tacos.web.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.InventoryReservation;
import tacos.OrderLine;
import tacos.ReservationStatus;
import tacos.ReservedIngredient;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;

//TC-16 - Reserva y libera inventario con updates atomicos
@Service
public class InventoryService {
    private final ReactiveMongoTemplate template;

    public InventoryService(ReactiveMongoTemplate template) {
        this.template = template;
    }

    public Mono<InventoryReservation> reserve(String reservationKey, List<OrderLine> lines) {
        return Mono.defer(() -> template.findById(reservationKey, InventoryReservation.class)
            .switchIfEmpty(startReservation(reservationKey, aggregate(lines))));
    }

    public Mono<InventoryReservation> confirm(String reservationKey, String orderId) {
        Query query = Query.query(Criteria.where("_id").is(reservationKey)
            .and("status").is(ReservationStatus.ACTIVE));
        Update update = new Update().set("status", ReservationStatus.COMPLETED)
            .set("orderId", orderId);
        return template.findAndModify(query, update,
            FindAndModifyOptions.options().returnNew(true), InventoryReservation.class)
            .switchIfEmpty(template.findById(reservationKey, InventoryReservation.class));
    }

    public Mono<Void> release(String reservationKey) {
        if (reservationKey == null) {
            return Mono.empty();
        }
        Query query = Query.query(Criteria.where("_id").is(reservationKey)
            .and("status").in(ReservationStatus.ACTIVE, ReservationStatus.COMPLETED));
        return template.findAndModify(query, new Update().set("status", ReservationStatus.RELEASED),
                FindAndModifyOptions.options().returnNew(false), InventoryReservation.class)
            .flatMap(reservation -> restore(reservation.getItems()))
            .then();
    }

    private Mono<InventoryReservation> startReservation(String key,
            List<ReservedIngredient> demand) {
        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(key);
        reservation.setReservationKey(key);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setItems(demand);
        List<ReservedIngredient> completed = new ArrayList<>();

        return template.insert(reservation)
            .flatMap(inserted -> Flux.fromIterable(demand)
                .concatMap(item -> decrement(item).doOnNext(ignored -> completed.add(item)))
                .then(markActive(key)))
            .onErrorResume(DuplicateKeyException.class,
                error -> template.findById(key, InventoryReservation.class))
            .onErrorResume(error -> restore(completed)
                .then(markReleased(key)).then(Mono.error(error)));
    }

    private Mono<UpdateResult> decrement(ReservedIngredient item) {
        Query query = Query.query(Criteria.where("_id").is(item.getIngredientId())
            .and("available").is(true)
            .and("stockOnHand").gte(item.getQuantity()));
        Update update = new Update().inc("stockOnHand", -item.getQuantity())
            .inc("version", 1);
        return template.updateFirst(query, update, Ingredient.class)
            .flatMap(result -> result.getModifiedCount() == 1 ? Mono.just(result)
                : Mono.error(new BusinessRuleException(ApiErrorCodes.INSUFFICIENT_STOCK,
                    "Insufficient stock for ingredient '" + item.getIngredientId() + "'.")));
    }

    private Mono<InventoryReservation> markActive(String key) {
        return template.findAndModify(Query.query(Criteria.where("_id").is(key)
                .and("status").is(ReservationStatus.PENDING)),
            new Update().set("status", ReservationStatus.ACTIVE),
            FindAndModifyOptions.options().returnNew(true), InventoryReservation.class);
    }

    private Mono<Void> markReleased(String key) {
        return template.updateFirst(Query.query(Criteria.where("_id").is(key)),
            new Update().set("status", ReservationStatus.RELEASED),
            InventoryReservation.class).then();
    }

    private Mono<Void> restore(List<ReservedIngredient> items) {
        return Flux.fromIterable(items)
            .concatMap(item -> template.updateFirst(
                Query.query(Criteria.where("_id").is(item.getIngredientId())),
                new Update().inc("stockOnHand", item.getQuantity())
                    .inc("version", 1), Ingredient.class))
            .then();
    }

    private List<ReservedIngredient> aggregate(List<OrderLine> lines) {
        Map<String, Integer> demand = new TreeMap<>();
        lines.forEach(line -> line.getTaco().getIngredients().forEach(ingredient ->
            demand.merge(ingredient.getId(), line.getQuantity(), Integer::sum)));
        List<ReservedIngredient> items = new ArrayList<>();
        demand.forEach((id, quantity) -> items.add(new ReservedIngredient(id, quantity)));
        return items;
    }
}
//Fin TC-16
