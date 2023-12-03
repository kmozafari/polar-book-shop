package com.polarbookshop.orderservice.event;

import com.polarbookshop.orderservice.order.domain.OrderService;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
public class OrderFunctions {

    private final Logger logger = LoggerFactory.getLogger(OrderFunctions.class);

    @Bean
    public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder(OrderService orderService) {
        return orderDispatchedMessage ->
                orderService
                        .consumeOrderDispatchedEvent(orderDispatchedMessage)
                        .doOnNext(
                                order ->
                                        logger.info(
                                                "The order with id {} is dispatched", order.id()))
                        .subscribe();
    }
}
