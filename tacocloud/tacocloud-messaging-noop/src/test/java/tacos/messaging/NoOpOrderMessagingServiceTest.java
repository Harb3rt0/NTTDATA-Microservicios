package tacos.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tacos.messaging.KitchenOrderEvent.KitchenTaco;

//TC-12 - El adaptador noop registra solo metadatos seguros
public class NoOpOrderMessagingServiceTest {

    @Test
    public void shouldNotLogCompleteOrderPayload() {
        Logger logger = (Logger) LoggerFactory.getLogger(NoOpOrderMessagingService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        KitchenOrderEvent event = new KitchenOrderEvent();
        event.setOrderId("ORDER1");
        event.setDeliveryStreet("PRIVATE_TEST_STREET");
        event.getTacos().add(new KitchenTaco());

        new NoOpOrderMessagingService().sendOrder(event);

        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message).isEqualTo("Sending order ORDER1 to kitchen with 1 tacos");
        assertThat(message).doesNotContain("PRIVATE_TEST_STREET");
        logger.detachAppender(appender);
    }
}
//Fin TC-12
